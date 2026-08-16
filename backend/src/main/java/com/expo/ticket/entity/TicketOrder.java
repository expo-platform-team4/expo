package com.expo.ticket.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ticket_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @Column(name = "member_user_id")
    private Long memberUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "orderer_type", nullable = false, length = 20)
    private TicketOrdererType ordererType;

    @Column(name = "ticket_subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal ticketSubtotalAmount;

    @Column(name = "booking_fee_rate", nullable = false, precision = 6, scale = 5)
    private BigDecimal bookingFeeRate;

    @Column(name = "booking_fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bookingFeeAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketOrderStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @OneToMany(mappedBy = "ticketOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketOrderItem> items = new ArrayList<>();

    /** 회원 주문을 결제 대기 상태로 생성한다. */
    public static TicketOrder createMemberOrder(
            String orderNumber,
            Long memberUserId,
            BigDecimal ticketSubtotalAmount,
            BigDecimal bookingFeeRate,
            BigDecimal bookingFeeAmount,
            BigDecimal totalAmount,
            int totalQuantity) {
        TicketOrder order = new TicketOrder();
        order.orderNumber = orderNumber;
        order.memberUserId = memberUserId;
        order.ordererType = TicketOrdererType.MEMBER;
        order.ticketSubtotalAmount = ticketSubtotalAmount;
        order.bookingFeeRate = bookingFeeRate;
        order.bookingFeeAmount = bookingFeeAmount;
        order.totalAmount = totalAmount;
        order.totalQuantity = totalQuantity;
        order.status = TicketOrderStatus.PENDING;
        return order;
    }

    /** 주문 항목을 추가하고 항목이 이 주문을 참조하도록 연결한다. */
    public void addItem(TicketOrderItem item) {
        items.add(item);
        item.assignOrder(this);
    }
}
