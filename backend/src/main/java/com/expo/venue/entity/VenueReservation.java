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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 예약 단일 원본.
 *
 * <p>모집공고 경로({@code RECRUITMENT_NOTICE})와 일반 박람회 등록 경로({@code EXPO_DIRECT})를 모두 담는다. 장소·홀·구역·기간은
 * 오직 이 테이블에만 저장하고, 기간 중복도 이 테이블(DB의 EXCLUDE 제약 + 계층 겹침 트리거)에서만 막는다.
 */
@Getter
@Entity
@Table(name = "venue_reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueReservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_source_type", nullable = false, length = 30)
    private ReservationSourceType reservationSourceType;

    @Column(name = "notice_request_id")
    private Long noticeRequestId;

    @Column(name = "opening_request_id")
    private Long openingRequestId;

    @Column(name = "recruitment_notice_id")
    private Long recruitmentNoticeId;

    @Column(name = "virtual_venue_id", nullable = false)
    private Long virtualVenueId;

    @Column(name = "venue_hall_id")
    private Long venueHallId;

    @Column(name = "venue_zone_id")
    private Long venueZoneId;

    @Column(name = "use_start_at", nullable = false)
    private Instant useStartAt;

    @Column(name = "use_end_at", nullable = false)
    private Instant useEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VenueReservationStatus status;

    @Column(name = "confirmed_by_admin_id", nullable = false)
    private Long confirmedByAdminId;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    /** 모집공고 생성 요청 경로의 확정 장소 예약 생성. */
    public static VenueReservation confirmForRecruitmentNotice(
            Long noticeRequestId,
            Long virtualVenueId,
            Long venueHallId,
            Long venueZoneId,
            Instant useStartAt,
            Instant useEndAt,
            Long confirmedByAdminId) {
        VenueReservation reservation = new VenueReservation();
        reservation.reservationSourceType = ReservationSourceType.RECRUITMENT_NOTICE;
        reservation.noticeRequestId = noticeRequestId;
        reservation.virtualVenueId = virtualVenueId;
        reservation.venueHallId = venueHallId;
        reservation.venueZoneId = venueZoneId;
        reservation.useStartAt = useStartAt;
        reservation.useEndAt = useEndAt;
        reservation.status = VenueReservationStatus.CONFIRMED;
        reservation.confirmedByAdminId = confirmedByAdminId;
        reservation.confirmedAt = Instant.now();
        return reservation;
    }

    /** 박람회 취소 등으로 확정 예약을 해제한다. */
    public void release() {
        this.status = VenueReservationStatus.RELEASED;
        this.releasedAt = Instant.now();
    }

    /** 모집공고 생성 완료 후, 그 공고에 이 예약을 연결한다. */
    public void linkToNotice(Long recruitmentNoticeId) {
        this.recruitmentNoticeId = recruitmentNoticeId;
    }
}
