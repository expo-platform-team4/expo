package com.expo.auth.repository;

import com.expo.auth.entity.PasswordResetStatus;
import com.expo.auth.entity.PasswordResetToken;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link PasswordResetToken} 리포지토리. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * 재요청 시 앞서 발급된(ISSUED) 토큰을 무효화하기 위해 조회한다.
     *
     * @param userId 대상 사용자 ID
     * @param status 조회할 상태 (재요청 처리에서는 {@link PasswordResetStatus#ISSUED})
     */
    List<PasswordResetToken> findByUserIdAndStatus(Long userId, PasswordResetStatus status);

    /**
     * 아직 유효한(ISSUED, 만료 전) 토큰 후보를 전부 조회한다.
     *
     * <p>토큰은 해시로만 저장되어 원문으로 직접 조회할 수 없다. {@link
     * com.expo.auth.service.PasswordResetService#confirmReset}에서 원문과 하나씩 대조한다.
     *
     * @param status 조회할 상태 ({@link PasswordResetStatus#ISSUED})
     * @param now 만료 기준 시각
     */
    List<PasswordResetToken> findByStatusAndExpiresAtAfter(PasswordResetStatus status, Instant now);
}
