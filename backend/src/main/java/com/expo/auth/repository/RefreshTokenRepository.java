package com.expo.auth.repository;

import com.expo.auth.entity.RefreshToken;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link RefreshToken} 리포지토리. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** 폐기되지 않고 만료되지 않은 Refresh Token 목록. 재발급 시 해시 대조에 사용한다. */
    List<RefreshToken> findByRevokedAtIsNullAndExpiresAtAfter(Instant now);
}
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link RefreshToken} 리포지토리. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {}
