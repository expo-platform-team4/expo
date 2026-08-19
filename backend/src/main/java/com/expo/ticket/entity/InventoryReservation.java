package com.expo.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결제 대기 중 티켓 재고를 임시 확보하는 예약. */
@Entity
@Table(name = "inventory_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_product_id", nullable = false)
    private TicketProduct ticketProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_order_id", nullable = false)
    private TicketOrder ticketOrder;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    /** 결제 대기 상태의 재고 예약을 생성한다. */
    @Builder
    private InventoryReservation(
            TicketProduct ticketProduct, TicketOrder ticketOrder, int quantity, Instant expiresAt) {
        this.ticketProduct = ticketProduct;
        this.ticketOrder = ticketOrder;
        this.quantity = quantity;
        this.status = InventoryReservationStatus.ACTIVE;
        this.reservedAt = Instant.now();
        this.expiresAt = expiresAt;
    }
}
