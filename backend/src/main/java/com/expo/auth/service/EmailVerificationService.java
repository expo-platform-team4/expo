package com.expo.auth.service;

import com.expo.auth.dto.EmailVerificationConfirmResponse;
import com.expo.auth.dto.EmailVerificationCreateResponse;
import com.expo.auth.entity.EmailVerification;
import com.expo.auth.entity.EmailVerificationStatus;
import com.expo.auth.repository.EmailVerificationRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 회원가입 이메일 본인인증 서비스.
 *
 * <p>휴대폰 인증({@link PhoneVerificationService})과 흐름은 같지만 둘이 다르다.
 *
 * <ul>
 *   <li>코드가 고정 테스트값이 아니라 매번 무작위로 생성돼 실제 메일로 나간다 ({@link EmailSender} 연동 완료 상태라
 *       가능하다)
 *   <li>인증 성공 뒤 발급하는 토큰을 회원가입({@code AuthService})이 실제로 소비·검증한다. 그래서 {@link
 *       #consumeForSignup} 이 따로 있고, 소비된 토큰은 {@link EmailVerificationStatus#USED} 로 표시해 재사용을 막는다
 * </ul>
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationProperties properties;
    private final EmailAvailabilityService emailAvailabilityService;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationRepository emailVerificationRepository,
            EmailVerificationProperties properties,
            EmailAvailabilityService emailAvailabilityService,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.properties = properties;
        this.emailAvailabilityService = emailAvailabilityService;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
    }

    /**
     * 이메일 본인인증을 요청한다.
     *
     * <p>동일 이메일의 기존 {@link EmailVerificationStatus#REQUESTED} 건은 만료 처리한 뒤 새 레코드를 생성한다 — 휴대폰
     * 인증 재요청과 같은 정책이다.
     */
    @Transactional
    public EmailVerificationCreateResponse requestVerification(String rawEmail) {
        Instant now = Instant.now();
        String normalized = emailAvailabilityService.normalize(rawEmail);

        emailVerificationRepository.expireRequestedByEmail(
                normalized, EmailVerificationStatus.REQUESTED, EmailVerificationStatus.EXPIRED);

        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);
        Instant expiresAt = now.plus(properties.getCodeExpireMinutes(), ChronoUnit.MINUTES);

        EmailVerification verification =
                EmailVerification.createRequested(normalized, codeHash, now, expiresAt);
        EmailVerification saved = emailVerificationRepository.save(verification);

        // 메일 발송은 커밋 이후로 미룬다 — 커밋 전에 보내면 저장 실패 시 "확인할 수 없는 인증코드"가
        // 나가버리고, SMTP 대기 시간만큼 트랜잭션도 불필요하게 길어진다.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendVerificationEmail(normalized, code);
                    }
                });

        // 개인정보·인증코드는 로그에 남기지 않는다 (AGENTS.md, DEBUG도 예외 없음).
        log.debug("이메일 본인인증 요청 verificationId={}", saved.getId());

        return new EmailVerificationCreateResponse(
                saved.getId(), normalized, expiresAt, "인증코드가 발송되었습니다.");
    }

    /**
     * 인증코드를 확인하고, 성공하면 회원가입용 1회성 토큰을 발급한다.
     *
     * <p>{@code noRollbackFor = BusinessException.class} — 만료·코드불일치로 실패할 때도
     * {@link EmailVerification#markExpired()}/{@link EmailVerification#markFailed()} 로 바꾼 상태를
     * 커밋해야 한다. 기본 롤백 정책대로 두면 unchecked 예외인 {@code BusinessException} 때문에 상태변경
     * 저장까지 롤백되어 레코드가 계속 {@code REQUESTED} 로 남고, 같은 인증 건에 인증코드를 무제한으로
     * 추측 시도할 수 있게 된다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public EmailVerificationConfirmResponse confirmVerification(
            Long verificationId, String verificationCode) {
        Instant now = Instant.now();

        EmailVerification verification =
                emailVerificationRepository
                        .findById(verificationId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (verification.getStatus() != EmailVerificationStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        }

        if (verification.isExpired(now)) {
            verification.markExpired();
            emailVerificationRepository.save(verification);
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!passwordEncoder.matches(verificationCode, verification.getVerificationCodeHash())) {
            verification.markFailed();
            emailVerificationRepository.save(verification);
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        String signupToken = UUID.randomUUID().toString();
        String signupTokenHash = passwordEncoder.encode(signupToken);
        Instant signupTokenExpiresAt =
                now.plus(properties.getSignupTokenExpireMinutes(), ChronoUnit.MINUTES);
        verification.markVerified(signupTokenHash, now, signupTokenExpiresAt);
        saveWithLockCheck(verification);

        return new EmailVerificationConfirmResponse(
                verification.getId(),
                verification.getEmail(),
                now,
                signupToken,
                "이메일 인증이 완료되었습니다.");
    }

    /**
     * 회원가입이 인증 완료 토큰을 소비한다.
     *
     * <p>{@code email}·{@code signupVerificationToken} 이 가입 폼에 입력된 이메일과 정확히 일치해야 한다 — 다른
     * 이메일로 인증받은 토큰을 여기 이메일에 쓰는 걸 막는다. 성공하면 상태를 {@link EmailVerificationStatus#USED} 로
     * 바꿔 재사용을 막는다.
     *
     * @throws BusinessException 일치하는 {@code VERIFIED} 건이 없으면 {@code EMAIL_VERIFICATION_TOKEN_INVALID}
     */
    @Transactional
    public void consumeForSignup(String rawEmail, String signupVerificationToken, Long userId) {
        String normalized = emailAvailabilityService.normalize(rawEmail);
        List<EmailVerification> candidates =
                emailVerificationRepository.findByEmailAndStatus(
                        normalized, EmailVerificationStatus.VERIFIED);

        EmailVerification matched =
                candidates.stream()
                        .filter(
                                candidate ->
                                        passwordEncoder.matches(
                                                signupVerificationToken,
                                                candidate.getSignupTokenHash()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID));

        Instant now = Instant.now();
        if (matched.isSignupTokenExpired(now)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
        }

        matched.markUsed(userId);
        saveWithLockCheck(matched);
    }

    /**
     * 낙관적 락({@code @Version}) 충돌을 공통 처리한다. 같은 인증 레코드에 대한 확인(confirm)·소비
     * (consume) 요청이 동시에 들어와도 하나만 성공시키기 위한 방어다 — 성공한 한 요청만 상태를
     * 실제로 바꾸고, 나머지는 저장 시점에 버전 충돌로 실패한다.
     */
    private void saveWithLockCheck(EmailVerification verification) {
        try {
            emailVerificationRepository.saveAndFlush(verification);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.RESOURCE_BUSY);
        }
    }

    private void sendVerificationEmail(String to, String code) {
        String subject = "[엑스포티켓] 이메일 인증코드";
        String body =
                """
                회원가입을 위한 이메일 인증코드입니다.

                아래 코드를 회원가입 화면에 입력해 주세요. 이 코드는 %d분간 유효합니다.

                인증코드: %s

                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """
                        .formatted(properties.getCodeExpireMinutes(), code);
        emailSender.send(to, subject, body);
    }

    /** {@code 000000}~{@code 999999} 사이의 6자리 숫자 코드. 앞자리가 0이어도 자리수를 맞춰 채운다. */
    private String generateCode() {
        int value = secureRandom.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", value);
    }
}
