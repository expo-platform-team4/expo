package com.expo.checkin.repository;

import com.expo.checkin.entity.TicketAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

/** QR 확인 링크 접근 토큰 영속성 접근 인터페이스. */
public interface TicketAccessTokenRepository extends JpaRepository<TicketAccessToken, Long> {}
