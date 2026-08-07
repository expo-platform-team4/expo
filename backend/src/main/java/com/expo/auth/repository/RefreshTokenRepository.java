package com.expo.auth.repository;

import com.expo.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link RefreshToken} 리포지토리. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 아직 폐기되지 않고 만료되지도 않은 Refresh Token을 모두 조회한다.
     *
     * <p>메서드 이름 규칙으로 Spring Data JPA가 아래 JPQL을 생성한다.
     *
     * <pre>{@code SELECT t FROM RefreshToken t WHERE t.revokedAt IS NULL AND t.expiresAt > ?1}</pre>
     *
     * <p>Refresh Token은 BCrypt 해시로만 저장되어 원문으로 조회할 수 없다. 그래서 후보를 받아 {@link
     * com.expo.auth.service.LoginService#reissueAccessToken(String)}에서 하나씩 대조한다.
     *
     * @param now 만료 기준 시각
     * @return 사용 가능한 Refresh Token 목록 (없으면 빈 리스트)
     */
    List<RefreshToken> findByRevokedAtIsNullAndExpiresAtAfter(Instant now);
}
