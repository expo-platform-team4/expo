package com.expo.auth.service;

import com.expo.auth.dto.LoginRequest;
import com.expo.auth.dto.LoginResponse;
import com.expo.auth.dto.TokenReissueResponse;
import com.expo.auth.entity.AccountStatus;
import com.expo.auth.entity.RefreshToken;
import com.expo.auth.entity.User;
import com.expo.auth.repository.RefreshTokenRepository;
import com.expo.auth.repository.UserRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
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
     * 이메일·비밀번호가 맞으면 JWT Access Token과 Refresh Token을 발급하고, 사용자 정보와 함께 돌려주는 로그인 메서드입니다.
     *
     *
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
                        // 유효하지 않은 이메일 또는 비밀번호입니다
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        // 탈퇴(WITHDRAWN) 계정은 로그인 불가 → HTTP 403 "탈퇴한 계정입니다."
        if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
            // 탈퇴한 계정입니다
            throw new BusinessException(ErrorCode.WITHDRAWN_ACCOUNT);
        }

        // 비밀번호 검증: DB 해시와 입력값 BCrypt 비교.
        // hash가 없거나 불일치 시 이메일 없음과 같은 INVALID_LOGIN_CREDENTIALS (401)로 응답.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // 유효하지 않은 이메일 또는 비밀번호입니다
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

        // 랜덤 문자열을 Refresh Token **원문(평문)**으로 만듭니다.
        String refreshTokenPlain = UUID.randomUUID().toString();
        // 랜덤 문자열을 BCrypt 해시로 만듭니다.
        String refreshTokenHash = passwordEncoder.encode(refreshTokenPlain);
        Instant refreshExpiresAt =
                now.plus(jwtProperties.getRefreshTokenExpireDays(), ChronoUnit.DAYS);

        // refresh_tokens 테이블에 INSERT (만료·폐기·IP 등은 MVP에서 null).
        refreshTokenRepository.save(
                RefreshToken.create(user.getId(), refreshTokenHash, refreshExpiresAt, null, null));

        // Controller가 ApiResponse로 감싸서 JSON 응답 (HTTP 200).
        // 로그인 결과를 한 덩어리로 묶어 LoginResponse로 반환
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

    /**
     * Refresh Token으로 Access Token을 재발급한다 (A-API-011).
     *
     * <p>요청 본문으로 전달된 Refresh Token(회원·클라이언트 사업자 등 로그인한 사용자 모두 동일)을 DB 해시와
     * 대조해 검증한다. 탈퇴 계정이 아니면 새 JWT만 발급하고, Refresh Token은 재발급하지 않는다. {@code
     * last_used_at}을 갱신한다.
     */
    @Transactional
    public TokenReissueResponse reissueAccessToken(String refreshTokenPlain) {
        // 만료·사용 시각 비교용 현재 시각
        Instant now = Instant.now();

        // DB에서 폐기되지 않고(revoked_at IS NULL) 아직 만료 전(expires_at > now)인 토큰 전부 조회.
        // 요청으로 전달된 원문과 BCrypt 해시를 대조해 일치하는 행을 찾는다 (MVP: 전체 스캔 + filter).
        // 없으면 INVALID_REFRESH_TOKEN (401 "유효하지 않은 Refresh Token입니다.").
        RefreshToken refreshToken =
                // 폐기되지 않고 만료되지 않은 Refresh Token 목록. 로그인 때 저장한 해시들 중, 지금 쓸 수 있는 것만 조회
                refreshTokenRepository.findByRevokedAtIsNullAndExpiresAtAfter(now).stream()
                        // 후보 전체에 대해 BCrypt 비교 — refreshTokenPlain은 요청 본문의 Refresh Token 원문
                        .filter(
                                token ->
                                        passwordEncoder.matches(
                                                refreshTokenPlain, token.getTokenHash()))
                        // 일치하는 토큰이 첫 번째로 찾아진 행을 Optional<RefreshToken>으로 반환
                        .findFirst()
                        // 일치하는 게 없으면 INVALID_REFRESH_TOKEN (401, "유효하지 않은 Refresh Token입니다.").
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // refresh_tokens.user_id로 users 조회. 사용자가 없으면 같은 INVALID_REFRESH_TOKEN.
        User user =
                userRepository
                        // 이 Refresh Token이 누구 것인지 users 테이블에서 PK로 조회
                        .findById(refreshToken.getUserId())
                        // Refresh Token은 있는데 User가 없으면 (데이터 꼬임 등) 같은 401로 응답
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 탈퇴 계정은 재발급 불가 → HTTP 403 "탈퇴한 계정입니다."
        if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.WITHDRAWN_ACCOUNT);
        }

        // 재발급 성공 → last_used_at 갱신 후 refresh_tokens UPDATE.
        refreshToken.recordUsage(now);
        refreshTokenRepository.save(refreshToken);

        // 새 Access Token(JWT) 발급. Refresh Token은 그대로 (재발급 API에서는 새로 만들지 않음).
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());

        // Controller가 ApiResponse로 감싸서 JSON 응답 (HTTP 200).
        // Access Token 재발급이 성공했을 때 클라이언트에 돌려줄 응답 DTO
        return new TokenReissueResponse(accessToken, jwtProperties.getAccessTokenExpireMinutes());
    }
}
