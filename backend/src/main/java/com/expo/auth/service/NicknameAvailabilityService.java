package com.expo.auth.service;

import com.expo.auth.dto.NicknameAvailabilityResponse;
import com.expo.auth.exception.InvalidNicknameException;
import com.expo.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 닉네임 사용 가능 여부 검증 서비스 (A-API-004).
 *
 * <p>회원가입(A-API-001, A-API-002)과 동일한 {@link UserRepository#existsByNickname(String)} 로 중복을 확인한다. 길이
 * 제약은 마이그레이션 {@code users.nickname VARCHAR(50) NOT NULL UNIQUE} 및 {@link
 * com.expo.auth.dto.SignupRequest} 의 {@code @Size(min=2, max=50)} 와 맞춘다.
 */
@Service
public class NicknameAvailabilityService {

  private static final int MIN_NICKNAME_LENGTH = 2;
  private static final int MAX_NICKNAME_LENGTH = 50;

  private final UserRepository userRepository;

  public NicknameAvailabilityService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * 입력값 trim 후 길이를 검증한다.
   *
   * @throws InvalidNicknameException null / 빈 문자열 / 2자 미만 / 50자 초과
   */
  public String normalize(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      throw new InvalidNicknameException("닉네임은 필수입니다.");
    }
    String trimmed = nickname.trim();
    if (trimmed.length() < MIN_NICKNAME_LENGTH || trimmed.length() > MAX_NICKNAME_LENGTH) {
      throw new InvalidNicknameException("닉네임은 2자 이상 50자 이하여야 합니다.");
    }
    return trimmed;
  }

  /**
   * 닉네임 사용 가능 여부를 확인한다.
   *
   * <p>형식·필수값 오류는 {@link InvalidNicknameException} 으로 던져 400 Bad Request 로 응답하고, 중복·사용 가능 판정은 응답
   * DTO 로 200 OK 반환한다.
   */
  @Transactional(readOnly = true)
  public NicknameAvailabilityResponse checkAvailability(String nickname) {
    String normalized = normalize(nickname);
    if (userRepository.existsByNickname(normalized)) {
      return NicknameAvailabilityResponse.duplicate(normalized);
    }
    return NicknameAvailabilityResponse.available(normalized);
  }
}
