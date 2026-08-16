package com.expo.ticket.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guest_order_infos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestOrder extends BaseTimeEntity {

    @Id private Long ticketOrderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_order_id")
    private TicketOrder ticketOrder;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = true)
    private Integer age;

    @Column(name = "lookup_password_hash", nullable = false, length = 255)
    private String lookupPasswordHash;

    @Column(name = "failed_lookup_count", nullable = false)
    private int failedLookupCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder
    private GuestOrder(
            TicketOrder ticketOrder,
            String guestName,
            String phoneNumber,
            Integer age,
            String lookupPasswordHash) {
        this.ticketOrder = ticketOrder;
        this.guestName = guestName;
        this.phoneNumber = phoneNumber;
        this.age = age;
        this.lookupPasswordHash = lookupPasswordHash;
        this.failedLookupCount = 0;
    }
}
