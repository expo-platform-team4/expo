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
    @Column(name = "default_included_items", columnDefinition = "jsonb")
    private String defaultIncludedItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 20)
    private OperationalStatus operationalStatus;
}
