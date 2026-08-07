package com.expo.auth.service;

import com.expo.auth.dto.EmailAvailabilityResponse;
import com.expo.auth.exception.InvalidEmailException;
import com.expo.auth.repository.UserRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 사용 가능 여부 검증 서비스 (A-API-003).
 *
 * <p>회원가입(A-API-001, A-API-002)과 동일한 {@link UserRepository#existsByEmail(String)} 로 중복을 확인한다.
 */
@Service
public class EmailAvailabilityService {

    private static final int MAX_EMAIL_LENGTH = 255;

    /** {@link com.expo.auth.dto.SignupRequest} 의 {@code @Email} 검증과 동일한 수준의 형식 검사. */
    private static final Pattern EMAIL_FORMAT =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // 이메일 중복 검증 서비스를 만들 때 DB 조회용 UserRepository를 Spring에게 받는 생성자
    private final UserRepository userRepository;

    public EmailAvailabilityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 입력값 trim 후 형식을 검증한다.
     *
     * @throws InvalidEmailException null / 빈 문자열 / 형식 불일치 / 255자 초과
     */
    public String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidEmailException("이메일을 입력해 주세요.");
        }
        String trimmed = email.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            throw new InvalidEmailException("이메일은 255자 이하여야 합니다.");
        }
        if (!EMAIL_FORMAT.matcher(trimmed).matches()) {
            throw new InvalidEmailException("올바른 이메일 형식이 아닙니다.");
        }
        // 정리된 이메일을 반환
        return trimmed;
    }

    /**
     * 이메일 사용 가능 여부를 확인한다.
     *
     * <p>형식·필수값 오류는 {@link InvalidEmailException} 으로 던져 400 Bad Request 로 응답하고, 중복·사용 가능 판정은 응답 DTO 로
     * 200 OK 반환한다.
     */
    @Transactional(readOnly = true)
    public EmailAvailabilityResponse checkAvailability(String email) {
        String normalized = normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            return EmailAvailabilityResponse.duplicate(normalized);
        }
        return EmailAvailabilityResponse.available(normalized);
    }
}
