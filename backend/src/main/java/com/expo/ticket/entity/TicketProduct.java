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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

/** TICKET_PRODUCTS 테이블 — 박람회에서 판매하는 표준 1일권 상품. */
@Getter
@Entity
@Table(name = "ticket_products")
public class TicketProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** expo 도메인 엔티티가 아직 없으므로 FK ID만 매핑한다. */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "sales_start_at", nullable = false)
    private Instant salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private Instant salesEndAt;

    @Column(name = "max_quantity_per_order", nullable = false)
    private int maxQuantityPerOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketProductStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(mappedBy = "ticketProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private TicketInventory inventory;

    public static TicketProduct create(
            Long expoId,
            String name,
            String description,
            BigDecimal price,
            Instant salesStartAt,
            Instant salesEndAt,
            int maxQuantityPerOrder) {
        TicketProduct product = new TicketProduct();
        product.expoId = expoId;
        product.name = name;
        product.description = description;
        product.price = price;
        product.salesStartAt = salesStartAt;
        product.salesEndAt = salesEndAt;
        product.maxQuantityPerOrder = maxQuantityPerOrder;
        product.status = TicketProductStatus.DRAFT;

        return product;
    }

    /** 상품 생성 시 만든 재고를 연결한다. */
    public void attachInventory(TicketInventory inventory) {
        this.inventory = inventory;
    }

    protected TicketProduct() {}
}
