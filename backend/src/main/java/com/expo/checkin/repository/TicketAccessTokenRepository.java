package com.expo.checkin.repository;

import com.expo.checkin.entity.TicketAccessToken;
import com.expo.checkin.entity.TicketAccessTokenScope;
import com.expo.checkin.entity.TicketAccessTokenStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** QR 확인 링크 접근 토큰 영속성 접근 인터페이스. */
public interface TicketAccessTokenRepository extends JpaRepository<TicketAccessToken, Long> {

    /**
     * 그 주문의 살아 있는 주문 조회 링크들. 최신 것이 앞에 온다.
     *
     * <p>알림 재발송이 쓴다 — 새 링크를 주기 전에 <b>이전 링크를 전부 끊기</b> 위해서다. 보통 한 건이지만
     * 목록으로 받는다. 재발송이 여러 번 일어났거나 과거에 여러 개가 생겼을 수 있고, 그때 하나만 끊으면
     * 살아 있는 링크가 남는다.
     *
     * <p>만료 여부는 여기서 보지 않는다. {@code status} 만 {@code ACTIVE} 면 끊는다 — 만료된 것을 또
     * 끊어도 손해가 없고, 시각 비교를 넣으면 경계에서 빠져나가는 행이 생긴다.
     */
    List<TicketAccessToken> findByTicketOrderIdAndScopeAndStatusOrderByIdDesc(
            Long ticketOrderId, TicketAccessTokenScope scope, TicketAccessTokenStatus status);
}
