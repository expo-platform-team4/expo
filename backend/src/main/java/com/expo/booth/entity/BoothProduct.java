package com.expo.booth.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 특정 모집공고에서 실제 판매되는 부스 상품.
 *
 * <p>{@link Booth} 가 공간이고, 이쪽이 그 공간을 파는 상품이다. {@code version} 은 결제·배정이 몰릴 때 동일 부스 상품이 중복
 * 판매되지 않도록 막는 낙관적 락 컬럼이라 {@link Version} 으로 매핑한다.
 */
@Getter
@Entity
@Table(name = "booth_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recruitment_notice_id", nullable = false)
    private Long recruitmentNoticeId;

    @Column(name = "booth_id", nullable = false)
    private Long boothId;

    @Column(name = "supply_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal supplyPrice;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "vat_included", nullable = false)
    private boolean vatIncluded = true;

    /** 제공 항목 스냅샷(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "included_items", columnDefinition = "jsonb")
    private String includedItems;

    @Column(name = "sales_start_at")
    private LocalDateTime salesStartAt;

    @Column(name = "sales_end_at")
    private LocalDateTime salesEndAt;

    @Column(name = "payment_enabled", nullable = false)
    private boolean paymentEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_status", nullable = false, length = 20)
    private BoothSalesStatus salesStatus;

    @Version
    @Column(nullable = false)
    private Long version;
}
