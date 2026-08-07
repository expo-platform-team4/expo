package com.expo.checkin.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ISSUED_TICKETS 테이블 — 결제 완료 후 구매 수량만큼 발급되는 개별 입장권.
 *
 * <p>QR 원문은 저장하지 않는다. {@code qrTokenHash} 만 두고, 원문은 조회 때마다 HMAC 으로 다시 만든다.
 */
@Getter
@Entity
@Table(name = "issued_tickets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedTicket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** order 도메인 엔티티가 아직 없으므로 FK ID만 매핑한다. */
    @Column(name = "ticket_order_item_id", nullable = false)
    private Long ticketOrderItemId;

    /** 체크인 시 대조할 박람회. expo 도메인 엔티티가 아직 없으므로 FK ID만 매핑한다. */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** {@code EXPO-생성일자-일련번호 6자리}. 현장에서 QR 대신 손으로 입력할 수도 있다. */
    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    private String ticketCode;

    /** QR 원문의 해시. 검증은 스캔한 원문을 해시해 이 값과 비교한다. */
    @Column(name = "qr_token_hash", nullable = false, unique = true, length = 255)
    private String qrTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedTicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    /** 같은 티켓이 동시에 두 번 체크인되는 것을 막는다. */
    @Version
    @Column(nullable = false)
    private Long version;
}
