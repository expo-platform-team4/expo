package com.expo.auth.service;


import com.expo.auth.dto.PhoneVerificationConfirmResponse;
import com.expo.auth.dto.PhoneVerificationCreateResponse;
import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;

import com.expo.auth.dto.PhoneVerificationCreateResponse;
import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;

import com.expo.auth.exception.InvalidPhoneNumberException;
import com.expo.auth.repository.PhoneVerificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 휴대폰 본인인증 서비스. PostgreSQL + JPA 로 저장한다. */

@Service
public class PhoneVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);

@Slf4j
@Service
public class PhoneVerificationService {

    // 010 같은 한국 휴대폰 번호만 받겠다”는 검증 규칙
    private static final Pattern PHONE_NUMBER = Pattern.compile("^01[016789]\\d{7,8}$");

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PhoneVerificationProperties properties;
    private final PasswordEncoder passwordEncoder;

    // Spring이 PhoneVerificationService를 만들 때 필요한 3가지를 주입받는 생성자입니다.
    public PhoneVerificationService(
            PhoneVerificationRepository phoneVerificationRepository,
            PhoneVerificationProperties properties,
            PasswordEncoder passwordEncoder) {
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 휴대폰 본인인증을 요청한다.
     *
     * <p>휴대폰 인증 요청 API가 호출됐을 때 서버가 순서대로 하는 처리 단계
     *휴대폰 인증 요청 API가 호출됐을 때 서버가 순서대로 하는 처리 단계
     *
     * <p>동일 번호의 기존 {@link PhoneVerificationStatus#REQUESTED} 건은 만료 처리한 뒤 새 레코드를 생성한다. MVP 단계에서는 외부
     * SMS API 를 호출하지 않으며, 테스트용 고정 인증번호({@code 123456})로 검증한다.
     */
    @Transactional
    public PhoneVerificationCreateResponse requestVerification(String phoneNumber) {
        // 지금 시각 (만료 시간·생성 시각 계산용)
        Instant now = Instant.now();
        // 번호 검증·정규화
        String normalized = normalize(phoneNumber);

        // 동일 번호로 이미 대기 중(REQUESTED)인 인증이 있으면 EXPIRED로 바꿔 무효화한다.
        // 재발송 시 "어떤 인증이 유효한지" 헷갈리지 않게 하기 위함.
        phoneVerificationRepository.expireRequestedByPhoneNumber(
                normalized, PhoneVerificationStatus.REQUESTED, PhoneVerificationStatus.EXPIRED);

        // 회원가입 API에서 "이 번호 인증 완료"를 증명할 때 쓰는 랜덤 토큰(평문) 생성.
        // DB에는 평문을 저장하지 않고 BCrypt 해시만 저장한다.
        String verificationToken = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(verificationToken);

        // 인증 유효 만료 시각 = 지금 + 설정값(기본 3분, application.yml / env로 변경 가능).
        Instant expiresAt = now.plus(properties.getCodeExpireMinutes(), ChronoUnit.MINUTES);

        // 아직 DB에 넣지 않은 PhoneVerification 엔티티 생성 (status=REQUESTED).
        PhoneVerification verification =
                PhoneVerification.createRequested(normalized, tokenHash, now, expiresAt);

        // phone_verifications 테이블에 INSERT. saved.getId()가 이후 확인 API의 verificationId.
        PhoneVerification saved = phoneVerificationRepository.save(verification);

        // MVP: SMS API 미연동 → 실제 문자는 안 가고, 테스트 인증번호는 DEBUG 로그로만 확인.
        log.debug(
                "휴대폰 본인인증 요청(MVP, SMS 미연동) phone={} verificationId={} testCode={}",
                normalized,
                saved.getId(),
                properties.getTestVerificationCode());

        // Controller가 AuthApiResponse로 감싸서 JSON 응답 (HTTP 200).
        // 휴대폰 인증 요청이 성공했을 때, 클라이언트에게 돌려줄 응답 DTO를 만들어 반환
        return new PhoneVerificationCreateResponse(
                saved.getId(), normalized, expiresAt, "인증번호가 발송되었습니다.");
    }

    /**
     * 휴대폰 본인인증 결과를 확인한다.
     *
     * <p>MVP 단계에서는 {@link PhoneVerificationProperties#getTestVerificationCode()} 와 일치하면 인증에 성공한다. 성공
     * 시 status 를 {@link PhoneVerificationStatus#VERIFIED} 로 갱신하고 회원가입용 토큰을 발급한다.
     */
    @Transactional
    public PhoneVerificationConfirmResponse confirmVerification(
            Long verificationId, String verificationCode) {
        // 만료·완료 시각 비교용 현재 시각
        Instant now = Instant.now();

        // 요청 API가 준 verificationId로 DB 조회. 없으면 PHONE_VERIFICATION_NOT_FOUND.
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

        // expiresAt이 지났으면 EXPIRED로 갱신 후 PHONE_VERIFICATION_EXPIRED.
        if (verification.isExpired(now)) {
            verification.markExpired();
            phoneVerificationRepository.save(verification);
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_EXPIRED);
        }

        // MVP: 설정된 테스트 인증번호(기본 123456)와 일치하는지 검증.
        // 불일치 시 FAILED로 갱신 후 PHONE_VERIFICATION_CODE_MISMATCH.
        if (!properties.getTestVerificationCode().equals(verificationCode)) {
            verification.markFailed();
            phoneVerificationRepository.save(verification);
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 → 회원가입 등 후속 API용 1회성 토큰(평문) 발급.
        // DB에는 BCrypt 해시만 저장하고, 평문 signupToken은 응답으로 클라이언트에만 전달.
        String signupToken = UUID.randomUUID().toString();
        String signupTokenHash = passwordEncoder.encode(signupToken);
        verification.markVerified(signupTokenHash, now);
        phoneVerificationRepository.save(verification);

        // Controller가 AuthApiResponse로 감싸서 JSON 응답 (HTTP 200).
        return new PhoneVerificationConfirmResponse(
                verification.getId(),
                verification.getPhoneNumber(),
                now,
                signupToken,
                "휴대폰 인증이 완료되었습니다.");
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
        String normalized = phoneNumber.trim().replaceAll("[^0-9]", "");
        if (!PHONE_NUMBER.matcher(normalized).matches()) {
            throw new InvalidPhoneNumberException("휴대폰 번호 형식이 올바르지 않습니다.");
        }
        return normalized;
    }
}
