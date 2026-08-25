package com.expo.auth.service;

import com.expo.auth.dto.PhoneVerificationConfirmResponse;
import com.expo.auth.dto.PhoneVerificationCreateResponse;
import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
import com.expo.auth.exception.InvalidPhoneNumberException;
import com.expo.auth.repository.PhoneVerificationRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.service.SmsSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 휴대폰 본인인증 서비스. PostgreSQL + JPA 로 저장한다. */
@Slf4j
@Service
public class PhoneVerificationService {

    // 010 같은 한국 휴대폰 번호만 받겠다”는 검증 규칙
    private static final Pattern PHONE_NUMBER = Pattern.compile("^01[016789]\\d{7,8}$");

    /** 인증번호 자릿수. 이메일 인증과 같다. */
    private static final int CODE_LENGTH = 6;

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PhoneVerificationProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(
            PhoneVerificationRepository phoneVerificationRepository,
            PhoneVerificationProperties properties,
            PasswordEncoder passwordEncoder,
            SmsSender smsSender) {
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
    }

    /**
     * 휴대폰 본인인증을 요청한다. 무작위 6자리 인증번호를 만들어 SMS 로 보낸다.
     *
     * <p>동일 번호의 기존 {@link PhoneVerificationStatus#REQUESTED} 건은 만료 처리한 뒤 새 레코드를 만든다.
     * 재발송 시 "어느 인증이 유효한지" 를 하나로 정하기 위해서다.
     *
     * <p>인증번호 원문은 어디에도 남기지 않는다 — DB 에는 BCrypt 해시만 넣고, 로그에도 찍지 않는다
     * (AGENTS.md 2절, DEBUG 도 예외 없음). 원문은 문자로 나가는 그 순간에만 존재한다.
     */
    @Transactional
    public PhoneVerificationCreateResponse requestVerification(String phoneNumber) {
        Instant now = Instant.now();
        String normalized = normalize(phoneNumber);

        // 간격 제한을 만료 처리보다 먼저 본다. 순서를 바꾸면 직전 건이 EXPIRED 로 바뀐 뒤에
        // 조회하게 되는데, 그래도 requestedAt 은 남으므로 결과는 같다. 다만 제한에 걸린 요청이
        // 멀쩡한 인증 건을 무효로 만들고 끝나는 것은 막아야 한다.
        Instant lastRequestedAt = phoneVerificationRepository.findLatestRequestedAt(normalized);
        if (lastRequestedAt != null) {
            Duration sinceLast = Duration.between(lastRequestedAt, now);
            if (sinceLast.getSeconds() < properties.getResendCooldownSeconds()) {
                throw new BusinessException(ErrorCode.PHONE_VERIFICATION_TOO_FREQUENT);
            }
        }

        phoneVerificationRepository.expireRequestedByPhoneNumber(
                normalized, PhoneVerificationStatus.REQUESTED, PhoneVerificationStatus.EXPIRED);

        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);
        Instant expiresAt = now.plus(properties.getCodeExpireMinutes(), ChronoUnit.MINUTES);

        PhoneVerification verification =
                PhoneVerification.createRequested(normalized, codeHash, now, expiresAt);
        PhoneVerification saved = phoneVerificationRepository.save(verification);

        // 문자 발송은 커밋 이후로 미룬다 — 커밋 전에 보내면 저장이 실패했을 때
        // "확인할 수 없는 인증번호" 가 이미 나가버리고, 대행사 왕복 시간만큼 트랜잭션도 길어진다.
        // 이메일 인증이 같은 이유로 같은 방식을 쓴다.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendVerificationSms(normalized, code);
                    }
                });

        log.debug("휴대폰 본인인증 요청 verificationId={}", saved.getId());

        return new PhoneVerificationCreateResponse(
                saved.getId(), normalized, expiresAt, "인증번호가 발송되었습니다.");
    }

    /**
     * 인증번호를 확인하고, 성공하면 회원가입용 1회성 토큰을 발급한다.
     *
     * <p>{@code noRollbackFor = BusinessException.class} — 만료·불일치로 실패할 때도
     * {@link PhoneVerification#markExpired()}/{@link PhoneVerification#markFailed()} 로 바꾼 상태가
     * 커밋되어야 한다. 기본 롤백 정책대로 두면 unchecked 예외인 {@code BusinessException} 때문에
     * 상태 변경까지 되돌아가 레코드가 계속 {@code REQUESTED} 로 남고, 같은 인증 건에 인증번호를
     * <b>무제한으로 추측</b>할 수 있게 된다. 고정 인증번호를 쓰던 동안에는 드러나지 않던 구멍이다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public PhoneVerificationConfirmResponse confirmVerification(
            Long verificationId, String verificationCode) {
        Instant now = Instant.now();

        PhoneVerification verification =
                phoneVerificationRepository
                        .findById(verificationId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PHONE_VERIFICATION_NOT_FOUND));

        // 이미 VERIFIED/EXPIRED/FAILED 등 대기(REQUESTED) 상태가 아니면 같은 NOT_FOUND로 응답.
        // (존재하지 않는 ID와 구분하지 않고, 재사용·중복 확인을 막는다.)
        if (verification.getStatus() != PhoneVerificationStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_NOT_FOUND);
        }

        if (verification.isExpired(now)) {
            verification.markExpired();
            phoneVerificationRepository.save(verification);
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_EXPIRED);
        }

        // 보낸 인증번호의 해시와 대조한다. 원문은 저장하지 않으므로 이 비교가 유일한 검증 수단이다.
        // 불일치 시 FAILED로 갱신 후 PHONE_VERIFICATION_CODE_MISMATCH.
        if (!passwordEncoder.matches(verificationCode, verification.getVerificationTokenHash())) {
            verification.markFailed();
            phoneVerificationRepository.save(verification);
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 → 회원가입 등 후속 API용 1회성 토큰(평문) 발급.
        // DB에는 BCrypt 해시만 저장하고, 평문 signupToken은 응답으로 클라이언트에만 전달.
        String signupToken = UUID.randomUUID().toString();
        String signupTokenHash = passwordEncoder.encode(signupToken);
        Instant signupTokenExpiresAt =
                now.plus(properties.getSignupTokenExpireMinutes(), ChronoUnit.MINUTES);
        verification.markVerified(signupTokenHash, now, signupTokenExpiresAt);
        phoneVerificationRepository.save(verification);

        // Controller가 ApiResponse로 감싸서 JSON 응답 (HTTP 200).
        return new PhoneVerificationConfirmResponse(
                verification.getId(),
                verification.getPhoneNumber(),
                now,
                signupToken,
                "휴대폰 인증이 완료되었습니다.");
    }

    /**
     * 회원가입이 휴대폰 인증 결과를 소비한다.
     *
     * <p>이게 없으면 인증은 화면의 버튼만 잠근다 — API 를 직접 부르면 인증하지 않은 번호로도
     * 가입할 수 있다. {@code EmailVerificationService#consumeForSignup} 과 같은 구조다.
     *
     * <p>가입토큰은 해시로만 저장하므로 번호로 후보를 좁힌 뒤 하나씩 대조한다. 원문으로 바로
     * 찾을 수 없는 것은 의도된 대가다 — DB 가 유출돼도 토큰을 복원할 수 없다.
     *
     * @throws BusinessException 일치하는 인증이 없거나, 가입토큰이 만료됐을 때
     */
    @Transactional
    public void consumeForSignup(String phoneNumber, String signupVerificationToken, Long userId) {
        String normalized = normalize(phoneNumber);
        List<PhoneVerification> candidates =
                phoneVerificationRepository.findByPhoneNumberAndStatus(
                        normalized, PhoneVerificationStatus.VERIFIED);

        PhoneVerification matched =
                candidates.stream()
                        .filter(
                                candidate ->
                                        candidate.getSignupTokenHash() != null
                                                && passwordEncoder.matches(
                                                        signupVerificationToken,
                                                        candidate.getSignupTokenHash()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PHONE_VERIFICATION_TOKEN_INVALID));

        if (matched.isSignupTokenExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_TOKEN_INVALID);
        }

        // USED 로 바꿔 같은 토큰으로 계정을 여러 개 만들지 못하게 한다.
        matched.markUsed(userId);
        phoneVerificationRepository.save(matched);
    }

    /**
     * 인증번호를 문자로 보낸다.
     *
     * <p>발송 실패는 예외로 올리지 않는다. {@link SmsSender} 가 예외 대신 결과로 실패를 돌려주는 계약이고,
     * 이 시점은 이미 커밋된 뒤라 되돌릴 것도 없다. 사용자는 유효시간 안에 재요청하면 된다.
     *
     * <p>실패해도 번호와 인증번호는 로그에 남기지 않는다. 남길 수 있는 건 식별자뿐이다.
     */
    private void sendVerificationSms(String phoneNumber, String code) {
        String text =
                """
        [expo] 인증번호 %s
        %d분 안에 입력해 주세요."""
                        .formatted(code, properties.getCodeExpireMinutes());

        var result = smsSender.send(phoneNumber, text);
        if (!result.success()) {
            log.warn("휴대폰 본인인증 문자 발송 실패 errorCode={}", result.errorCode());
        }
    }

    /** {@code 000000}~{@code 999999} 사이의 6자리 숫자 코드. 앞자리가 0이어도 자리수를 맞춰 채운다. */
    private String generateCode() {
        int value = secureRandom.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", value);
    }

    /**
     * 입력값을 숫자만 추출한 뒤 국내 휴대폰 번호 형식으로 정규화한다.
     *
     * @throws InvalidPhoneNumberException 형식 불일치
     */
    public String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidPhoneNumberException("휴대폰 번호를 입력해 주세요.");
        }
        String normalized = phoneNumber.trim().replaceAll("\\D", "");
        if (!PHONE_NUMBER.matcher(normalized).matches()) {
            throw new InvalidPhoneNumberException("휴대폰 번호 형식이 올바르지 않습니다.");
        }
        return normalized;
    }
}
