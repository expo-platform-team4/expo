package com.expo.auth.service;

import com.expo.auth.dto.PasswordResetConfirmResponse;
import com.expo.auth.dto.PasswordResetRequestResponse;
import com.expo.auth.entity.PasswordResetStatus;
import com.expo.auth.entity.PasswordResetToken;
import com.expo.auth.entity.User;
import com.expo.auth.repository.PasswordResetTokenRepository;
import com.expo.auth.repository.UserRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 비밀번호 재설정 서비스 (A-API-013, A-API-014). */
@Slf4j
@Service
public class PasswordResetService {

    /** 재설정 토큰 유효 시간. MVP 단계라 상수로 고정한다 (필요해지면 설정값으로 뺀다). */
    private static final long TOKEN_EXPIRE_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailAvailabilityService emailAvailabilityService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailAvailabilityService emailAvailabilityService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailAvailabilityService = emailAvailabilityService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 비밀번호 재설정을 요청한다 (A-API-013).
     *
     * <p>이메일 존재 여부를 응답으로 노출하지 않는다(계정 존재 여부 추측 방지) — 가입된 이메일이면 토큰을 발급하고,
     * 아니면 아무 것도 하지 않은 채 같은 성공 메시지를 돌려준다.
     *
     * <p>MVP 단계: 실제 이메일 발송 미연동. 개인정보·토큰은 로그에 남기지 않으므로(AGENTS.md), 이메일 연동
     * 전까지는 이 흐름을 로컬에서 끝까지 수동 테스트할 방법이 없다 — 발급 자체는 정상 동작함을 단위 테스트로
     * 확인한다.
     */
    @Transactional
    public PasswordResetRequestResponse requestReset(String rawEmail) {
        String email = emailAvailabilityService.normalize(rawEmail);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_EXPIRE_MINUTES, ChronoUnit.MINUTES);

        userRepository
                .findByEmail(email)
                .ifPresent(
                        user -> {
                            // 같은 사용자의 기존 발급(ISSUED) 토큰은 재요청 시 폐기해 "어떤 토큰이 유효한지"
                            // 헷갈리지 않게 한다 (휴대폰 본인인증 재요청과 동일한 정책).
                            passwordResetTokenRepository
                                    .findByUserIdAndStatus(user.getId(), PasswordResetStatus.ISSUED)
                                    .forEach(
                                            token -> {
                                                token.markRevoked();
                                                passwordResetTokenRepository.save(token);
                                            });

                            String resetToken = UUID.randomUUID().toString();
                            String tokenHash = passwordEncoder.encode(resetToken);
                            PasswordResetToken issued =
                                    PasswordResetToken.issue(
                                            user.getId(), tokenHash, now, expiresAt, null);
                            passwordResetTokenRepository.save(issued);

                            // 개인정보·토큰은 로그에 남기지 않는다 (AGENTS.md, DEBUG도 예외 없음).
                            // MVP라 이메일 발송이 없어 이 로그만으로는 재설정 토큰을 알 수 없다 —
                            // 실제 이메일 연동 전까지는 이 흐름을 로컬에서 끝까지 수동 테스트할 방법이 없다.
                            log.debug("비밀번호 재설정 요청(MVP, 이메일 미연동) userId={}", user.getId());
                        });

        return new PasswordResetRequestResponse(
                email, expiresAt, "해당 이메일로 가입된 계정이 있으면 재설정 안내를 보냈습니다.");
    }

    /**
     * 재설정 토큰을 확인하고 새 비밀번호를 저장한다 (A-API-014).
     *
     * @throws BusinessException 토큰이 없거나({@code PASSWORD_RESET_TOKEN_NOT_FOUND}), 만료됐거나({@code
     *     PASSWORD_RESET_TOKEN_EXPIRED}), 새 비밀번호 확인이 불일치({@code PASSWORD_MISMATCH})
     */
    @Transactional
    public PasswordResetConfirmResponse confirmReset(
            String resetTokenPlain, String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        Instant now = Instant.now();

        PasswordResetToken token =
                passwordResetTokenRepository
                        .findByStatusAndExpiresAtAfter(PasswordResetStatus.ISSUED, now)
                        .stream()
                        .filter(
                                candidate ->
                                        passwordEncoder.matches(
                                                resetTokenPlain, candidate.getTokenHash()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND));

        // findByStatusAndExpiresAtAfter가 만료 전 토큰만 걸러주지만, 방어적으로 한 번 더 확인한다.
        if (token.isExpired(now)) {
            token.markExpired();
            passwordResetTokenRepository.save(token);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        User user =
                userRepository
                        .findById(token.getUserId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markUsed(now);
        passwordResetTokenRepository.save(token);

        return new PasswordResetConfirmResponse("비밀번호가 재설정되었습니다.");
    }
}
