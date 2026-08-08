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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 재사용 가능한 부스 형태와 기본 크기·제공 항목 템플릿. */
@Getter
@Entity
@Table(name = "booth_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shape_code", nullable = false, unique = true, length = 30)
    private String shapeCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal width;

    @Column(precision = 8, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal depth;

    @Column(name = "dimension_unit", nullable = false, length = 10)
    private String dimensionUnit = "M";

    /** 기본 제공 항목 스냅샷(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_included_items", columnDefinition = "jsonb")
    private String defaultIncludedItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 20)
    private OperationalStatus operationalStatus;

    /** 부스 템플릿 등록. 운영 상태는 ACTIVE 로 고정한다. */
    public static BoothTemplate create(
            String shapeCode,
            String name,
            BigDecimal width,
            BigDecimal height,
            BigDecimal depth,
            String dimensionUnit,
            String defaultIncludedItems) {
        BoothTemplate template = new BoothTemplate();
        template.shapeCode = shapeCode;
        template.name = name;
        template.width = width;
        template.height = height;
        template.depth = depth;
        if (dimensionUnit != null) {
            template.dimensionUnit = dimensionUnit;
        }
        template.defaultIncludedItems = defaultIncludedItems;
        template.operationalStatus = OperationalStatus.ACTIVE;
        return template;
    }
}
