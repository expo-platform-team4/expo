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

/** 구역 도면에 존재하는 고정 부스 공간 원본. 상품이 아니라 공간이다. */
@Getter
@Entity
@Table(name = "booths")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venue_zone_id", nullable = false)
    private Long venueZoneId;

    @Column(name = "booth_template_id")
    private Long boothTemplateId;

    @Column(name = "booth_number", nullable = false, length = 30)
    private String boothNumber;

    @Column(name = "shape_code", nullable = false, length = 30)
    private String shapeCode;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal width;

    @Column(precision = 8, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal depth;

    @Column(name = "dimension_unit", nullable = false, length = 10)
    private String dimensionUnit = "M";

    @Column(name = "position_x", precision = 10, scale = 2)
    private BigDecimal positionX;

    @Column(name = "position_y", precision = 10, scale = 2)
    private BigDecimal positionY;

    @Column(name = "rotation_degree", precision = 6, scale = 2)
    private BigDecimal rotationDegree = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 20)
    private OperationalStatus operationalStatus;
}
