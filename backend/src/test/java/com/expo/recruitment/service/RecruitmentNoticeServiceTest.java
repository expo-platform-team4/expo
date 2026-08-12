package com.expo.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentNoticeConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.dto.UpdateRecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.entity.VenueDecision;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.entity.VenueReservation;
import com.expo.venue.repository.VenueReservationRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link RecruitmentNoticeService} 의 승인 상태 검증과 초안·게시·마감 상태 전이 규칙을 검증한다. */
class RecruitmentNoticeServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long REQUEST_ID = 10L;
    private static final Long RESERVATION_ID = 20L;
    private static final Long HOST_CLIENT_ID = 30L;
    private static final Instant T1 = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-09-30T00:00:00Z");
    private static final Instant T3 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant T4 = Instant.parse("2026-10-05T00:00:00Z");

    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private VenueReservationRepository venueReservationRepository;
    private RecruitmentNoticeService service;

    @BeforeEach
    void setUp() {
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        recruitmentNoticeRequestRepository = mock(RecruitmentNoticeRequestRepository.class);
        venueReservationRepository = mock(VenueReservationRepository.class);
        service =
                new RecruitmentNoticeService(
                        recruitmentNoticeRepository,
                        recruitmentNoticeRequestRepository,
                        venueReservationRepository,
                        new RecruitmentNoticeConverter());
    }

    private CreateRecruitmentNoticeRequest createRequest() {
        return new CreateRecruitmentNoticeRequest(
                REQUEST_ID, RESERVATION_ID, "공고 제목", "공고 내용", null, null, T1, T2);
    }

    private RecruitmentNoticeRequest allowedNoticeRequest() {
        RecruitmentNoticeRequest entity =
                RecruitmentNoticeRequest.create(HOST_CLIENT_ID, "제목", "설명", T1, T2, T3, T4, 1L);
        entity.decideVenue(VenueDecision.ALLOWED, ADMIN_ID, "충돌 없음");
        return entity;
    }

    private VenueReservation confirmedReservation(Long noticeRequestId) {
        return VenueReservation.confirmForRecruitmentNotice(
                noticeRequestId, 1L, null, null, T3, T4, ADMIN_ID);
    }

    @Test
    void createRejectsWhenApplicationPeriodInvalid() {
        CreateRecruitmentNoticeRequest request =
                new CreateRecruitmentNoticeRequest(
                        REQUEST_ID, RESERVATION_ID, "제목", "내용", null, null, T2, T1);

        assertThatThrownBy(() -> service.create(ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_PERIOD_INVALID);
        verify(recruitmentNoticeRequestRepository, never()).findById(any());
    }

    @Test
    void createRejectsWhenNoticeRequestNotFound() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND);
    }

    @Test
    void createRejectsWhenVenueDecisionNotAllowed() {
        RecruitmentNoticeRequest pending =
                RecruitmentNoticeRequest.create(HOST_CLIENT_ID, "제목", "설명", T1, T2, T3, T4, 1L);
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_CREATION_NOT_ALLOWED);
    }

    @Test
    void createRejectsWhenNoticeAlreadyExistsForRequest() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_RECRUITMENT_NOTICE_REQUEST);
    }

    @Test
    void createRejectsWhenReservationNotFound() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
    }

    /** 다른 요청의 확정 예약 ID를 잘못 지정한 경우를 거부해야 한다. */
    @Test
    void createRejectsWhenReservationBelongsToAnotherRequest() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(REQUEST_ID + 1)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
    }

    /** 이미 해제된 예약을 연결하려는 경우를 거부해야 한다. */
    @Test
    void createRejectsWhenReservationAlreadyReleased() {
        VenueReservation released = confirmedReservation(REQUEST_ID);
        released.release();
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(released));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENUE_RESERVATION_ALREADY_RELEASED);
    }

    @Test
    void createSucceedsAsDraft() {
        when(recruitmentNoticeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(allowedNoticeRequest()));
        when(recruitmentNoticeRepository.existsByRequestId(REQUEST_ID)).thenReturn(false);
        when(venueReservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(REQUEST_ID)));
        when(recruitmentNoticeRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecruitmentNoticeResponse response = service.create(ADMIN_ID, createRequest());

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.DRAFT);
        assertThat(response.hostClientId()).isEqualTo(HOST_CLIENT_ID);
    }

    private RecruitmentNotice draftNotice() {
        return RecruitmentNotice.create(
                REQUEST_ID, HOST_CLIENT_ID, RESERVATION_ID, "제목", "내용", T1, T2, ADMIN_ID);
    }

    @Test
    void updateRejectsWhenNotDraft() {
        RecruitmentNotice notice = draftNotice();
        notice.publish();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        UpdateRecruitmentNoticeRequest request =
                new UpdateRecruitmentNoticeRequest("새 제목", "새 내용", null, null, T1, T2);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_EDITABLE);
    }

    @Test
    void updateSucceedsWhenDraft() {
        RecruitmentNotice notice = draftNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        UpdateRecruitmentNoticeRequest request =
                new UpdateRecruitmentNoticeRequest("새 제목", "새 내용", null, null, T1, T2);

        RecruitmentNoticeResponse response = service.update(1L, request);

        assertThat(response.title()).isEqualTo("새 제목");
    }

    @Test
    void publishRejectsWhenNotDraft() {
        RecruitmentNotice notice = draftNotice();
        notice.publish();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.publish(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_PUBLISHABLE);
    }

    @Test
    void publishSucceedsFromDraft() {
        RecruitmentNotice notice = draftNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        RecruitmentNoticeResponse response = service.publish(1L);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.OPEN);
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    void closeRejectsWhenNotOpen() {
        RecruitmentNotice notice = draftNotice();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.close(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_CLOSABLE);
    }

    @Test
    void closeSucceedsFromOpen() {
        RecruitmentNotice notice = draftNotice();
        notice.publish();
        when(recruitmentNoticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        RecruitmentNoticeResponse response = service.close(1L);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.CLOSED);
    }

    @Test
    void listPublicReturnsOnlyOpenNotices() {
        RecruitmentNotice published = draftNotice();
        published.publish();
        when(recruitmentNoticeRepository.findAllByStatus(RecruitmentNoticeStatus.OPEN))
                .thenReturn(java.util.List.of(published));

        assertThat(service.listPublic()).hasSize(1);
        assertThat(service.listPublic().get(0).status()).isEqualTo(RecruitmentNoticeStatus.OPEN);
    }

    /** 초안·마감 등 게시 중이 아닌 공고는 공개 상세 조회에서 볼 수 없어야 한다. */
    @Test
    void getPublicRejectsWhenNotOpen() {
        when(recruitmentNoticeRepository.findByIdAndStatus(1L, RecruitmentNoticeStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublic(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
    }

    @Test
    void getPublicReturnsOpenNotice() {
        RecruitmentNotice published = draftNotice();
        published.publish();
        when(recruitmentNoticeRepository.findByIdAndStatus(1L, RecruitmentNoticeStatus.OPEN))
                .thenReturn(Optional.of(published));

        RecruitmentNoticeResponse response = service.getPublic(1L);

        assertThat(response.status()).isEqualTo(RecruitmentNoticeStatus.OPEN);
    }
}
