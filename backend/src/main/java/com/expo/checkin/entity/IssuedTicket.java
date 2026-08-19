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

    private IssuedTicket(
            Long ticketOrderItemId,
            Long expoId,
            String ticketCode,
            String qrTokenHash,
            Instant issuedAt) {
        this.ticketOrderItemId = ticketOrderItemId;
        this.expoId = expoId;
        this.ticketCode = ticketCode;
        this.qrTokenHash = qrTokenHash;
        this.issuedAt = issuedAt;
        this.status = IssuedTicketStatus.ISSUED;
    }

    /**
     * 입장권 한 장을 발권한다.
     *
     * <p>QR 해시까지 한 번에 받는다. QR 원문이 {@code ticketCode} 만으로 정해지므로 INSERT 전에 계산할 수 있다 — 티켓 {@code
     * id} 를 서명에 넣었다면 INSERT 뒤에야 값이 정해져서 임시값을 넣었다 덮어쓰는 2단계 쓰기가 됐을 것이다.
     *
     * @param issuedAt 발권 시각. 한 주문의 티켓이 같은 값을 갖도록 호출부에서 넘긴다
     */
    public static IssuedTicket issue(
            Long ticketOrderItemId,
            Long expoId,
            String ticketCode,
            String qrTokenHash,
            Instant issuedAt) {
        return new IssuedTicket(ticketOrderItemId, expoId, ticketCode, qrTokenHash, issuedAt);
    }

    /** 입장에 쓸 수 있는 상태인가. 환불·무효 티켓은 QR 이 유효해도 들여보내면 안 된다. */
    public boolean isUsable() {
        return status == IssuedTicketStatus.ISSUED;
    }

    /** 이미 입장한 티켓인가. 재입장 시도를 가려낸다. */
    public boolean isCheckedIn() {
        return status == IssuedTicketStatus.CHECKED_IN;
    }

    /**
     * 현장 입장 처리. 되돌릴 수 없다.
     *
     * <p>호출 전에 {@link #isUsable()} 로 확인해야 한다. 상태 판정은 이력을 함께 남겨야 해서 서비스가 하고, 여기서는 전이만 한다.
     */
    public void checkIn(Instant checkedAt) {
        this.status = IssuedTicketStatus.CHECKED_IN;
        this.checkedInAt = checkedAt;
    }
}
