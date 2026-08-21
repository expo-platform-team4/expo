package com.expo.auth.repository;

import com.expo.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 특정 사용자의 아직 폐기되지 않은 Refresh Token을 모두 조회한다 (전체 기기 로그아웃용, A-API-012).
     *
     * @param userId 대상 사용자 ID
     * @return 폐기 대상 Refresh Token 목록 (없으면 빈 리스트)
     */
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    /**
     * 아직 폐기되지 않은 경우에만 last_used_at을 갱신한다 (재발급, A-API-011).
     *
     * <p>재발급과 로그아웃(A-API-012)이 같은 토큰을 동시에 건드릴 때의 경합을 막는다. 후보 조회 →
     * 메모리에서 검증 → 별도 UPDATE로 저장하는 흐름에서, 그 사이 로그아웃이 먼저 커밋해 버리면 단순
     * {@code save()}는 로그아웃이 채운 {@code revoked_at}을 다시 {@code null}로 덮어쓸 수 있다.
     * {@code revoked_at IS NULL} 조건을 WHERE 절에 넣어 DB가 원자적으로 확인·갱신하게 하면, 로그아웃이
     * 먼저 커밋된 경우 이 UPDATE는 0행에 적용되어 재발급이 실패한다.
     *
     * @param id 대상 Refresh Token PK
     * @param now 갱신 시각
     * @return 갱신된 행 수. 0이면 이미 폐기됐거나 존재하지 않는 토큰.
     */
    @Modifying
    @Query(
            "UPDATE RefreshToken t SET t.lastUsedAt = :now "
                    + "WHERE t.id = :id AND t.revokedAt IS NULL")
    int markUsedIfActive(@Param("id") Long id, @Param("now") Instant now);
}
