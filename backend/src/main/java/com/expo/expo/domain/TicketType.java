package com.expo.expo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 티켓 종류(TICKET_TYPE) 엔티티 — EXPO_TICKET_테이블정의서_v2.xlsx / TICKET_TYPE 시트 기반
 *
 * 박람회 승인 후 관리자 대시보드에서 등록하는 가격대별 티켓 종류 및 재고 관리 테이블.
 * 목록 카드의 "15,000원부터" 표기는 MIN(price), '매진' 배지는 모든 종류가
 * sold_quantity = total_quantity 일 때로 판정한다(비고 1·2).
 */
@Entity
@Table(
    name = "ticket_type",
    indexes = @Index(name = "idx_ticket_type_expo", columnList = "expo_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketType {

    /** 티켓 종류 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id")
    private Long ticketTypeId;

    /** 소속 박람회 — FK(expo.expo_id) */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** 티켓명 (예: 일반권, 얼리버드, VIP) */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 티켓 단가(원) */
    @Column(name = "price", nullable = false)
    private Integer price;

    /** 판매 가능한 총 수량 */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    /** 누적 판매 수량 (잔여 = total_quantity - sold_quantity) */
    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private TicketType(Long expoId, String name, Integer price, Integer totalQuantity) {
        this.expoId = expoId;
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.soldQuantity = 0;
    }

    /** 잔여 수량 */
    public int getRemainingQuantity() {
        return totalQuantity - soldQuantity;
    }

    /** 매진 여부 */
    public boolean isSoldOut() {
        return soldQuantity >= totalQuantity;
    }
}
