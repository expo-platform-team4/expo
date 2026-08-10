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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 플랫폼이 보유한 행사장·전시장 원본. */
@Getter
@Entity
@Table(name = "virtual_venues")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualVenue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "region_code", nullable = false, length = 30)
    private String regionCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "map_file_id")
    private Long mapFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 20)
    private OperationalStatus operationalStatus;

    /** 가상 장소 등록. 운영 상태는 ACTIVE 로 고정한다. */
    public static VirtualVenue create(
            String name, String address, String regionCode, String description, Long mapFileId) {
        VirtualVenue venue = new VirtualVenue();
        venue.name = name;
        venue.address = address;
        venue.regionCode = regionCode;
        venue.description = description;
        venue.mapFileId = mapFileId;
        venue.operationalStatus = OperationalStatus.ACTIVE;
        return venue;
    }
}
