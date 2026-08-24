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

/** 가상 장소 안의 전시장·홀 단위. */
@Getter
@Entity
@Table(name = "venue_halls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueHall extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "hall_code", nullable = false, length = 30)
    private String hallCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    @Column(precision = 10, scale = 2)
    private BigDecimal depth;

    @Column(name = "layout_file_id")
    private Long layoutFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 20)
    private OperationalStatus operationalStatus;

    /** 장소 안에 홀 등록. 운영 상태는 ACTIVE 로 고정한다. */
    public static VenueHall create(
            Long venueId,
            String hallCode,
            String name,
            BigDecimal width,
            BigDecimal depth,
            Long layoutFileId) {
        VenueHall hall = new VenueHall();
        hall.venueId = venueId;
        hall.hallCode = hallCode;
        hall.name = name;
        hall.width = width;
        hall.depth = depth;
        hall.layoutFileId = layoutFileId;
        hall.operationalStatus = OperationalStatus.ACTIVE;
        return hall;
    }

    /** 배치도 파일 교체. */
    public void updateLayout(Long layoutFileId) {
        this.layoutFileId = layoutFileId;
    }
}
