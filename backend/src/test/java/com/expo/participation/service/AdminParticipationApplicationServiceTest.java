package com.expo.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.converter.ApplicationOperationHistoryConverter;
import com.expo.participation.converter.ParticipationApplicationConverter;
import com.expo.participation.dto.AdminParticipationApplicationResponse;
import com.expo.participation.dto.ApplicationOperationHistoryResponse;
import com.expo.participation.entity.ApplicationOperationActionType;
import com.expo.participation.entity.ApplicationOperationHistory;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ApplicationOperationHistoryRepository;
import com.expo.participation.repository.ParticipationApplicationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link AdminParticipationApplicationService} 의 운영 확인·보완 요청 처리 규칙을 검증한다. */
class AdminParticipationApplicationServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long APPLICATION_ID = 10L;
    private static final Long NOTICE_ID = 20L;

    private ParticipationApplicationRepository participationApplicationRepository;
    private ApplicationOperationHistoryRepository applicationOperationHistoryRepository;
    private AdminParticipationApplicationService service;

    @BeforeEach
    void setUp() {
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        applicationOperationHistoryRepository = mock(ApplicationOperationHistoryRepository.class);
        service =
                new AdminParticipationApplicationService(
                        participationApplicationRepository,
                        applicationOperationHistoryRepository,
                        new ParticipationApplicationConverter(),
                        new ApplicationOperationHistoryConverter());
    }

    private ParticipationApplication application() {
        return ParticipationApplication.create(NOTICE_ID, 30L, "테스트 참가기업", null, null, null);
    }

    @Test
    void listFiltersByRecruitmentNoticeIdWhenProvided() {
        when(participationApplicationRepository.findAllByRecruitmentNoticeId(NOTICE_ID))
                .thenReturn(List.of(application()));

        List<AdminParticipationApplicationResponse> responses = service.list(NOTICE_ID);

        assertThat(responses).hasSize(1);
        verify(participationApplicationRepository).findAllByRecruitmentNoticeId(NOTICE_ID);
    }

    @Test
    void listReturnsAllWhenRecruitmentNoticeIdMissing() {
        when(participationApplicationRepository.findAll()).thenReturn(List.of(application()));

        List<AdminParticipationApplicationResponse> responses = service.list(null);

        assertThat(responses).hasSize(1);
        verify(participationApplicationRepository).findAll();
    }

    @Test
    void getRejectsWhenNotFound() {
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(APPLICATION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND);
    }

    @Test
    void checkRecordsCheckedHistoryAndTimestamps() {
        ParticipationApplication application = application();
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        AdminParticipationApplicationResponse response =
                service.check(APPLICATION_ID, ADMIN_ID, "확인함");

        assertThat(response.adminCheckedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.adminCheckedAt()).isNotNull();
        verify(applicationOperationHistoryRepository)
                .save(
                        argThatActionType(
                                ApplicationOperationActionType.CHECKED, APPLICATION_ID, ADMIN_ID));
    }

    @Test
    void requestCorrectionRecordsHistory() {
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application()));

        service.requestCorrection(APPLICATION_ID, ADMIN_ID, "사업자등록증 다시 첨부해주세요");

        verify(applicationOperationHistoryRepository)
                .save(
                        argThatActionType(
                                ApplicationOperationActionType.CORRECTION_REQUESTED,
                                APPLICATION_ID,
                                ADMIN_ID));
    }

    @Test
    void completeCorrectionRejectsWhenNoCorrectionWasRequested() {
        when(participationApplicationRepository.findByIdForUpdate(APPLICATION_ID))
                .thenReturn(Optional.of(application()));
        when(applicationOperationHistoryRepository
                        .findFirstByApplicationIdOrderByCreatedAtDescIdDesc(APPLICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeCorrection(APPLICATION_ID, ADMIN_ID, "완료"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CORRECTION_NOT_REQUESTED);
    }

    @Test
    void completeCorrectionRejectsWhenLatestHistoryIsNotCorrectionRequest() {
        when(participationApplicationRepository.findByIdForUpdate(APPLICATION_ID))
                .thenReturn(Optional.of(application()));
        when(applicationOperationHistoryRepository
                        .findFirstByApplicationIdOrderByCreatedAtDescIdDesc(APPLICATION_ID))
                .thenReturn(
                        Optional.of(
                                ApplicationOperationHistory.create(
                                        APPLICATION_ID,
                                        ApplicationOperationActionType.CHECKED,
                                        null,
                                        ADMIN_ID)));

        assertThatThrownBy(() -> service.completeCorrection(APPLICATION_ID, ADMIN_ID, "완료"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CORRECTION_NOT_REQUESTED);
    }

    @Test
    void completeCorrectionSucceedsWhenLatestHistoryIsCorrectionRequest() {
        when(participationApplicationRepository.findByIdForUpdate(APPLICATION_ID))
                .thenReturn(Optional.of(application()));
        when(applicationOperationHistoryRepository
                        .findFirstByApplicationIdOrderByCreatedAtDescIdDesc(APPLICATION_ID))
                .thenReturn(
                        Optional.of(
                                ApplicationOperationHistory.create(
                                        APPLICATION_ID,
                                        ApplicationOperationActionType.CORRECTION_REQUESTED,
                                        "보완 요청",
                                        ADMIN_ID)));

        service.completeCorrection(APPLICATION_ID, ADMIN_ID, "보완 완료됨");

        verify(applicationOperationHistoryRepository)
                .save(
                        argThatActionType(
                                ApplicationOperationActionType.CORRECTION_COMPLETED,
                                APPLICATION_ID,
                                ADMIN_ID));
    }

    @Test
    void updateMemoUpdatesEntityAndRecordsHistory() {
        ParticipationApplication application = application();
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        AdminParticipationApplicationResponse response =
                service.updateMemo(APPLICATION_ID, ADMIN_ID, "우수 참가기업");

        assertThat(response.adminMemo()).isEqualTo("우수 참가기업");
        verify(applicationOperationHistoryRepository)
                .save(
                        argThatActionType(
                                ApplicationOperationActionType.MEMO_UPDATED,
                                APPLICATION_ID,
                                ADMIN_ID));
    }

    @Test
    void listHistoryRejectsWhenApplicationNotFound() {
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listHistory(APPLICATION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND);
    }

    @Test
    void listHistoryReturnsHistoriesNewestFirst() {
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application()));
        when(applicationOperationHistoryRepository.findAllByApplicationIdOrderByCreatedAtDescIdDesc(
                        APPLICATION_ID))
                .thenReturn(
                        List.of(
                                ApplicationOperationHistory.create(
                                        APPLICATION_ID,
                                        ApplicationOperationActionType.CHECKED,
                                        null,
                                        ADMIN_ID)));

        List<ApplicationOperationHistoryResponse> histories = service.listHistory(APPLICATION_ID);

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).actionType()).isEqualTo(ApplicationOperationActionType.CHECKED);
    }

    private static ApplicationOperationHistory argThatActionType(
            ApplicationOperationActionType actionType, Long applicationId, Long adminId) {
        return org.mockito.ArgumentMatchers.argThat(
                history ->
                        history.getActionType() == actionType
                                && history.getApplicationId().equals(applicationId)
                                && history.getProcessedByAdminId().equals(adminId));
    }
}
