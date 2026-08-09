package com.expo.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

/** TICKET_INVENTORIES 테이블 — 티켓 상품별 재고를 동시성 안전하게 관리한다. */
@Getter
@Entity
@Table(name = "ticket_inventories")
public class TicketInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "ticket_product_id", nullable = false, unique = true)
    private TicketProduct ticketProduct;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    /** PostgreSQL 생성 컬럼. 애플리케이션에서 직접 변경하지 않는다. */
    @Column(name = "available_quantity", insertable = false, updatable = false)
    private int availableQuantity;

    @Version
    @Column(nullable = false)
    private Long version;

    public static TicketInventory create(TicketProduct ticketProduct, int totalQuantity) {
        TicketInventory inventory = new TicketInventory();
        inventory.ticketProduct = ticketProduct;
        inventory.totalQuantity = totalQuantity;
        inventory.reservedQuantity = 0;
        inventory.soldQuantity = 0;
        return inventory;
    }

    protected TicketInventory() {}
}
