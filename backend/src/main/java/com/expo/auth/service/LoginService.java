package com.expo.auth.service;

import com.expo.auth.dto.LoginRequest;
import com.expo.auth.dto.LoginResponse;
import com.expo.auth.entity.AccountStatus;
import com.expo.auth.entity.RefreshToken;
import com.expo.auth.entity.User;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.repository.RefreshTokenRepository;
import com.expo.auth.repository.UserRepository;
import com.expo.jwt.JwtProperties;
import com.expo.jwt.JwtTokenProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이메일·비밀번호 로그인 서비스 (A-API-008). */
@Service
public class LoginService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;

  public LoginService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
  }

  /**
   * 이메일·비밀번호로 로그인하고 JWT Access Token·Refresh Token을 발급한다.
   *
   * <p>Refresh Token 원문은 클라이언트에 반환하고, DB에는 BCrypt 해시만 저장한다 (V1 refresh_tokens).
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    // 이메일로 DB 조회. 반환: Optional<User> — 있으면 User, 없으면 빈 Optional.
    User user =
        userRepository
            .findByEmail(request.email().trim())
            //유효하지 않은 이메일 또는 비밀번호입니다
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

    // 탈퇴(WITHDRAWN) 계정은 로그인 불가 → HTTP 403 "탈퇴한 계정입니다."
    if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
      //탈퇴한 계정입니다
      throw new BusinessException(ErrorCode.WITHDRAWN_ACCOUNT);
    }

    // 비밀번호 검증: DB 해시와 입력값 BCrypt 비교.
    // hash가 없거나 불일치 시 이메일 없음과 같은 INVALID_LOGIN_CREDENTIALS (401)로 응답.
    if (user.getPasswordHash() == null
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
          //유효하지 않은 이메일 또는 비밀번호입니다
      throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }

    // 로그인 성공 → last_login_at 갱신 후 users 테이블 UPDATE.
    Instant now = Instant.now();
    user.recordLogin(now);
    userRepository.save(user);

    // Access Token(JWT): userId·role을 담아 API 인증에 사용.
    // 로그인 성공 후 API 호출에 쓰는 JWT Access Token을 만드는 코드
    String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());

    // Refresh Token(원문): 클라이언트에 반환. DB에는 BCrypt 해시만 저장.
    String refreshTokenPlain = UUID.randomUUID().toString();
    String refreshTokenHash = passwordEncoder.encode(refreshTokenPlain);
    Instant refreshExpiresAt = now.plus(jwtProperties.getRefreshTokenExpireDays(), ChronoUnit.DAYS);

    // refresh_tokens 테이블에 INSERT (만료·폐기·IP 등은 MVP에서 null).
    refreshTokenRepository.save(
        RefreshToken.create(user.getId(), refreshTokenHash, refreshExpiresAt, null, null));

    // Controller가 AuthApiResponse로 감싸서 JSON 응답 (HTTP 200).
    return new LoginResponse(
        accessToken,
        jwtProperties.getAccessTokenExpireMinutes(),
        refreshTokenPlain,
        refreshExpiresAt,
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getRole());
  }
}
