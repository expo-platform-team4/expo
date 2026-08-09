package com.expo.recruitment.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentNoticeConverter;
import com.expo.recruitment.dto.CreateRecruitmentNoticeRequest;
import com.expo.recruitment.dto.RecruitmentNoticeResponse;
import com.expo.recruitment.dto.UpdateRecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentNoticeRequestRepository;
import com.expo.venue.repository.VenueReservationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentNoticeService {

    private final RecruitmentNoticeRepository recruitmentNoticeRepository;
    private final RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository;
    private final VenueReservationRepository venueReservationRepository;
    private final RecruitmentNoticeConverter recruitmentNoticeConverter;

    public RecruitmentNoticeService(
            RecruitmentNoticeRepository recruitmentNoticeRepository,
            RecruitmentNoticeRequestRepository recruitmentNoticeRequestRepository,
            VenueReservationRepository venueReservationRepository,
            RecruitmentNoticeConverter recruitmentNoticeConverter) {
        this.recruitmentNoticeRepository = recruitmentNoticeRepository;
        this.recruitmentNoticeRequestRepository = recruitmentNoticeRequestRepository;
        this.venueReservationRepository = venueReservationRepository;
        this.recruitmentNoticeConverter = recruitmentNoticeConverter;
    }

    /** 기업 모집 공고 초안 생성. 근거 요청의 주최 클라이언트를 그대로 연결한다. */
    @Transactional
    public RecruitmentNoticeResponse create(
            Long createdByAdminId, CreateRecruitmentNoticeRequest request) {
        RecruitmentNoticeRequest noticeRequest =
                recruitmentNoticeRequestRepository
                        .findById(request.requestId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_REQUEST_NOT_FOUND));
        if (recruitmentNoticeRepository.existsByRequestId(request.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RECRUITMENT_NOTICE_REQUEST);
        }
        if (!venueReservationRepository.existsById(request.venueReservationId())) {
            throw new BusinessException(ErrorCode.VENUE_RESERVATION_NOT_FOUND);
        }
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                                request.requestId(),
                                noticeRequest.getHostClientId(),
                                request.venueReservationId(),
                                request.title(),
                                request.content(),
                                request.applicationStartAt(),
                                request.applicationEndAt(),
                                createdByAdminId)
                        .withDetails(request.eligibility(), request.submissionRequirements());
        RecruitmentNotice saved = recruitmentNoticeRepository.save(notice);
        return recruitmentNoticeConverter.toResponse(saved);
    }

    /** 관리자용 기업 모집 공고 목록 조회. */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeResponse> list() {
        return recruitmentNoticeRepository.findAll().stream()
                .map(recruitmentNoticeConverter::toResponse)
                .toList();
    }

    /** 관리자용 기업 모집 공고 상세 조회. */
    @Transactional(readOnly = true)
    public RecruitmentNoticeResponse get(Long noticeId) {
        return recruitmentNoticeConverter.toResponse(getEntity(noticeId));
    }

    /** 공고 내용·조건 수정. 초안 상태에서만 가능하다. */
    @Transactional
    public RecruitmentNoticeResponse update(Long noticeId, UpdateRecruitmentNoticeRequest request) {
        RecruitmentNotice notice = getEntity(noticeId);
        if (notice.getStatus() != RecruitmentNoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_EDITABLE);
        }
        notice.update(
                request.title(),
                request.content(),
                request.eligibility(),
                request.submissionRequirements(),
                request.applicationStartAt(),
                request.applicationEndAt());
        return recruitmentNoticeConverter.toResponse(notice);
    }

    /** 공고 게시. 초안 상태에서만 가능하다. */
    @Transactional
    public RecruitmentNoticeResponse publish(Long noticeId) {
        RecruitmentNotice notice = getEntity(noticeId);
        if (notice.getStatus() != RecruitmentNoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_PUBLISHABLE);
        }
        notice.publish();
        return recruitmentNoticeConverter.toResponse(notice);
    }

    /** 기업 모집 조기 마감. 게시 중인 공고만 마감할 수 있다. */
    @Transactional
    public RecruitmentNoticeResponse close(Long noticeId) {
        RecruitmentNotice notice = getEntity(noticeId);
        if (notice.getStatus() != RecruitmentNoticeStatus.OPEN) {
            throw new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_CLOSABLE);
        }
        notice.close();
        return recruitmentNoticeConverter.toResponse(notice);
    }

    private RecruitmentNotice getEntity(Long noticeId) {
        return recruitmentNoticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND));
    }

    /** 게시 중인 기업 모집 공고 목록 조회 (공개). */
    @Transactional(readOnly = true)
    public List<RecruitmentNoticeResponse> listPublic() {
        return recruitmentNoticeRepository.findAllByStatus(RecruitmentNoticeStatus.OPEN).stream()
                .map(recruitmentNoticeConverter::toResponse)
                .toList();
    }

    /** 기업 모집 공고 상세 조회 (공개). 초안·취소된 공고는 조회할 수 없다. */
    @Transactional(readOnly = true)
    public RecruitmentNoticeResponse getPublic(Long noticeId) {
        RecruitmentNotice notice =
                recruitmentNoticeRepository
                        .findByIdAndStatusNotIn(
                                noticeId,
                                List.of(
                                        RecruitmentNoticeStatus.DRAFT,
                                        RecruitmentNoticeStatus.CANCELED))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND));
        return recruitmentNoticeConverter.toResponse(notice);
    }
}
