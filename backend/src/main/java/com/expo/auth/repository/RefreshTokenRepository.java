package com.expo.auth.repository;

import com.expo.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link RefreshToken} 리포지토리. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {}
