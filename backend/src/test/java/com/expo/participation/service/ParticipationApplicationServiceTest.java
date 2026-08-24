package com.expo.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.converter.ParticipationApplicationConverter;
import com.expo.participation.dto.CreateParticipationApplicationRequest;
import com.expo.participation.dto.ParticipationApplicationResponse;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link ParticipationApplicationService} 의 모집공고 게시 상태·부스 상품 존재 여부 검증을 확인한다. */
class ParticipationApplicationServiceTest {

    private static final Long CLIENT_USER_ID = 1L;
    private static final Long NOTICE_ID = 10L;
    private static final Long BOOTH_PRODUCT_ID = 20L;

    private ParticipationApplicationRepository participationApplicationRepository;
    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private BoothProductRepository boothProductRepository;
    private ParticipationApplicationService service;

    @BeforeEach
    void setUp() {
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        boothProductRepository = mock(BoothProductRepository.class);
        service =
                new ParticipationApplicationService(
                        participationApplicationRepository,
                        recruitmentNoticeRepository,
                        boothProductRepository,
                        new ParticipationApplicationConverter());
    }

    private CreateParticipationApplicationRequest requestWithBoothProduct(Long boothProductId) {
        return new CreateParticipationApplicationRequest(
                NOTICE_ID, "테스트 참가기업", null, null, boothProductId);
    }

    /** 이 브랜치의 엔티티에는 아직 팩토리 메서드가 없어 리플렉션으로 id·status·hostClientId 만 세팅한다. */
    private RecruitmentNotice noticeWithStatus(RecruitmentNoticeStatus status) {
        return noticeWithStatus(status, null);
    }

    private RecruitmentNotice noticeWithStatus(RecruitmentNoticeStatus status, Long hostClientId) {
        try {
            Constructor<RecruitmentNotice> constructor =
                    RecruitmentNotice.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            RecruitmentNotice notice = constructor.newInstance();
            Field idField = RecruitmentNotice.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notice, NOTICE_ID);
            Field statusField = RecruitmentNotice.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(notice, status);
            Field hostClientIdField = RecruitmentNotice.class.getDeclaredField("hostClientId");
            hostClientIdField.setAccessible(true);
            hostClientIdField.set(notice, hostClientId);
            return notice;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void createRejectsWhenNoticeNotFound() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CLIENT_USER_ID, requestWithBoothProduct(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
        verify(participationApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsWhenNoticeNotOpen() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.DRAFT)));

        assertThatThrownBy(() -> service.create(CLIENT_USER_ID, requestWithBoothProduct(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_OPEN);
    }

    @Test
    void createRejectsWhenApplyingToOwnNotice() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(
                        Optional.of(
                                noticeWithStatus(RecruitmentNoticeStatus.OPEN, CLIENT_USER_ID)));

        assertThatThrownBy(() -> service.create(CLIENT_USER_ID, requestWithBoothProduct(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_APPLY_TO_OWN_NOTICE);
        verify(participationApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsWhenDuplicateActiveApplicationExists() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(participationApplicationRepository
                        .existsByRecruitmentNoticeIdAndClientUserIdAndStatusIn(
                                eq(NOTICE_ID), eq(CLIENT_USER_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(CLIENT_USER_ID, requestWithBoothProduct(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_PARTICIPATION_APPLICATION);
        verify(participationApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsWhenBoothProductNotFound() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothProductRepository.existsByIdAndRecruitmentNoticeId(BOOTH_PRODUCT_ID, NOTICE_ID))
                .thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        CLIENT_USER_ID, requestWithBoothProduct(BOOTH_PRODUCT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_FOUND);
    }

    @Test
    void createSucceedsWithBoothProductFromSameNotice() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(boothProductRepository.existsByIdAndRecruitmentNoticeId(BOOTH_PRODUCT_ID, NOTICE_ID))
                .thenReturn(true);
        when(participationApplicationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipationApplicationResponse response =
                service.create(CLIENT_USER_ID, requestWithBoothProduct(BOOTH_PRODUCT_ID));

        assertThat(response.selectedBoothProductId()).isEqualTo(BOOTH_PRODUCT_ID);
    }

    @Test
    void createSucceedsWithoutBoothProduct() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(noticeWithStatus(RecruitmentNoticeStatus.OPEN)));
        when(participationApplicationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipationApplicationResponse response =
                service.create(CLIENT_USER_ID, requestWithBoothProduct(null));

        assertThat(response.recruitmentNoticeId()).isEqualTo(NOTICE_ID);
        assertThat(response.companyNameSnapshot()).isEqualTo("테스트 참가기업");
    }

    @Test
    void getMineRejectsWhenNotFoundOrNotOwned() {
        when(participationApplicationRepository.findByIdAndClientUserId(1L, CLIENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(1L, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND);
    }

    @Test
    void getMineSucceeds() {
        ParticipationApplication application =
                ParticipationApplication.create(
                        NOTICE_ID, CLIENT_USER_ID, "테스트 참가기업", null, null, null);
        when(participationApplicationRepository.findByIdAndClientUserId(1L, CLIENT_USER_ID))
                .thenReturn(Optional.of(application));

        ParticipationApplicationResponse response = service.getMine(1L, CLIENT_USER_ID);

        assertThat(response.companyNameSnapshot()).isEqualTo("테스트 참가기업");
    }
}
