package com.expo.auth.service;

import com.expo.auth.converter.UserConverter;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.entity.User;
import com.expo.auth.repository.UserRepository;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final UserConverter userConverter;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserRepository userRepository, UserConverter userConverter, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.userConverter = userConverter;
    this.passwordEncoder = passwordEncoder;
  }

  /** 일반 회원 로컬 회원가입. role 은 MEMBER, account_status 는 ACTIVE 로 고정한다. */
  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    }
    if (userRepository.existsByNickname(request.nickname())) {
      throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
    }

    String passwordHash = passwordEncoder.encode(request.password());
    User user = User.createMember(request.email(), passwordHash, request.nickname());
    User saved = userRepository.save(user);

    return userConverter.toSignupResponse(saved);
  }
}
