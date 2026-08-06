package com.expo.venue.entity;

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

/** 홀 안에서 부스가 배치되는 구역. */
@Getter
@Entity
@Table(name = "venue_zones")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueZone extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hall_id", nullable = false)
    private Long hallId;

    @Column(name = "zone_code", nullable = false, length = 30)
    private String zoneCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_booth_count", nullable = false)
    private Integer maxBoothCount;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    @Column(precision = 10, scale = 2)
    private BigDecimal depth;

    @Column(name = "layout_file_id")
    private Long layoutFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 20)
    private OperationalStatus operationalStatus;
}
