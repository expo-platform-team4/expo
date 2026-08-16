package com.expo.ticket.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 주문 당시 티켓 상품의 가격과 수량을 보존하는 주문 항목. */
@Entity
@Table(name = "ticket_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketOrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_order_id", nullable = false)
    private TicketOrder ticketOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_product_id", nullable = false)
    private TicketProduct ticketProduct;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "item_subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal itemSubtotalAmount;

    /** 주문 시점의 단가와 수량으로 주문 항목을 생성한다. */
    public static TicketOrderItem create(
            TicketProduct ticketProduct, int quantity, BigDecimal unitPrice) {
        TicketOrderItem item = new TicketOrderItem();
        item.ticketProduct = ticketProduct;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        item.itemSubtotalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    void assignOrder(TicketOrder ticketOrder) {
        this.ticketOrder = ticketOrder;
    }
}
