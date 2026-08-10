package com.expo.checkin.entity;

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
 * CHECK_IN_HISTORIES 테이블 — QR 또는 티켓 코드 검증·입장 처리 결과.
 *
 * <p>성공만이 아니라 실패({@link CheckInResult#ALREADY_USED} 등)도 남긴다.
 *
 * <p>이 테이블은 {@code created_at}/{@code updated_at} 이 없고 {@code checked_at} 하나만 갖는 append-only 이벤트
 * 로그라 {@code BaseTimeEntity} 를 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "check_in_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckInHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issued_ticket_id", nullable = false)
    private Long issuedTicketId;

    /** expo 도메인 엔티티가 아직 없으므로 FK ID만 매핑한다. */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** 체크인을 처리한 클라이언트 (client_profiles.user_id). */
    @Column(name = "processed_by_client_id", nullable = false)
    private Long processedByClientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckInMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CheckInResult result;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(columnDefinition = "TEXT")
    private String detail;
}
