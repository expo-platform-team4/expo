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

    /** 홀 안에 구역 등록. 운영 상태는 ACTIVE 로 고정한다. */
    public static VenueZone create(
            Long hallId,
            String zoneCode,
            String name,
            Integer maxBoothCount,
            BigDecimal width,
            BigDecimal depth,
            Long layoutFileId) {
        VenueZone zone = new VenueZone();
        zone.hallId = hallId;
        zone.zoneCode = zoneCode;
        zone.name = name;
        zone.maxBoothCount = maxBoothCount;
        zone.width = width;
        zone.depth = depth;
        zone.layoutFileId = layoutFileId;
        zone.operationalStatus = OperationalStatus.ACTIVE;
        return zone;
    }

    /** 배치도 파일 교체. */
    public void updateLayout(Long layoutFileId) {
        this.layoutFileId = layoutFileId;
    }
}
