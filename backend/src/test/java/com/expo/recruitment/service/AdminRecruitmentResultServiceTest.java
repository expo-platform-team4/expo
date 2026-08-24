package com.expo.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothOrderRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import com.expo.recruitment.converter.RecruitmentResultConverter;
import com.expo.recruitment.converter.RecruitmentResultItemConverter;
import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentResult;
import com.expo.recruitment.entity.RecruitmentResultStatus;
import com.expo.recruitment.repository.RecruitmentNoticeRepository;
import com.expo.recruitment.repository.RecruitmentResultItemRepository;
import com.expo.recruitment.repository.RecruitmentResultRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link AdminRecruitmentResultService} 의 생성·전달·취소 규칙을 확인한다. */
class AdminRecruitmentResultServiceTest {

    private static final Long NOTICE_ID = 1L;
    private static final Long HOST_CLIENT_ID = 2L;
    private static final Long RESULT_ID = 3L;
    private static final Long APPLICATION_ID = 4L;
    private static final Long CLIENT_USER_ID = 5L;
    private static final Long ALLOCATION_ID = 6L;
    private static final Long ORDER_ID = 7L;
    private static final Long BOOTH_PRODUCT_ID = 8L;

    private RecruitmentNoticeRepository recruitmentNoticeRepository;
    private RecruitmentResultRepository recruitmentResultRepository;
    private RecruitmentResultItemRepository recruitmentResultItemRepository;
    private ParticipationApplicationRepository participationApplicationRepository;
    private BoothAllocationRepository boothAllocationRepository;
    private BoothOrderRepository boothOrderRepository;
    private AdminRecruitmentResultService service;

    @BeforeEach
    void setUp() {
        recruitmentNoticeRepository = mock(RecruitmentNoticeRepository.class);
        recruitmentResultRepository = mock(RecruitmentResultRepository.class);
        recruitmentResultItemRepository = mock(RecruitmentResultItemRepository.class);
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        boothOrderRepository = mock(BoothOrderRepository.class);
        service =
                new AdminRecruitmentResultService(
                        recruitmentNoticeRepository,
                        recruitmentResultRepository,
                        recruitmentResultItemRepository,
                        participationApplicationRepository,
                        boothAllocationRepository,
                        boothOrderRepository,
                        new RecruitmentResultConverter(new RecruitmentResultItemConverter()));
    }

    private static void withId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private RecruitmentNotice closedNotice() {
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                        10L,
                        HOST_CLIENT_ID,
                        "제목",
                        "내용",
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(1)),
                        99L);
        notice.publish();
        notice.close();
        return notice;
    }

    @Test
    void generateRejectsWhenNoticeNotFound() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(NOTICE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_FOUND);
    }

    @Test
    void generateRejectsWhenNoticeNotClosed() {
        RecruitmentNotice notice =
                RecruitmentNotice.create(
                        10L,
                        HOST_CLIENT_ID,
                        "제목",
                        "내용",
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(1)),
                        99L);
        when(recruitmentNoticeRepository.findById(NOTICE_ID)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.generate(NOTICE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_NOTICE_NOT_CLOSED);
    }

    @Test
    void generateRejectsWhenAlreadyGenerated() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(closedNotice()));
        when(recruitmentResultRepository.existsByRecruitmentNoticeId(NOTICE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.generate(NOTICE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_RECRUITMENT_RESULT);
    }

    @Test
    void generateAggregatesOnlyAssignedAllocations() {
        when(recruitmentNoticeRepository.findById(NOTICE_ID))
                .thenReturn(Optional.of(closedNotice()));
        when(recruitmentResultRepository.existsByRecruitmentNoticeId(NOTICE_ID)).thenReturn(false);

        ParticipationApplication application =
                ParticipationApplication.create(
                        NOTICE_ID, CLIENT_USER_ID, "회사", null, null, BOOTH_PRODUCT_ID);
        withId(application, APPLICATION_ID);
        when(participationApplicationRepository.findAllByRecruitmentNoticeIdAndStatus(
                        NOTICE_ID, ParticipationApplicationStatus.SUBMITTED))
                .thenReturn(List.of(application));

        BoothAllocation allocation =
                BoothAllocation.create(APPLICATION_ID, ORDER_ID, BOOTH_PRODUCT_ID, CLIENT_USER_ID);
        withId(allocation, ALLOCATION_ID);
        when(boothAllocationRepository.findAllByApplicationIdIn(List.of(APPLICATION_ID)))
                .thenReturn(List.of(allocation));

        BoothOrder order =
                BoothOrder.create(
                        APPLICATION_ID,
                        CLIENT_USER_ID,
                        BOOTH_PRODUCT_ID,
                        "ORDER-1",
                        new BigDecimal("100000"),
                        "idem-1",
                        Instant.now().plus(Duration.ofMinutes(10)));
        withId(order, ORDER_ID);
        when(boothOrderRepository.findAllById(List.of(ORDER_ID))).thenReturn(List.of(order));

        when(recruitmentResultRepository.saveAndFlush(any()))
                .thenAnswer(
                        inv -> {
                            RecruitmentResult saved = inv.getArgument(0);
                            withId(saved, RESULT_ID);
                            return saved;
                        });

        var response = service.generate(NOTICE_ID);

        assertThat(response.confirmedCompanyCount()).isEqualTo(1);
        assertThat(response.confirmedBoothCount()).isEqualTo(1);
        assertThat(response.totalBoothSalesAmount()).isEqualByComparingTo("100000");
        assertThat(response.status()).isEqualTo(RecruitmentResultStatus.DELIVERED);
    }

    @Test
    void cancelRejectsWhenConfirmed() {
        RecruitmentResult result =
                RecruitmentResult.create(NOTICE_ID, HOST_CLIENT_ID, 0, 0, BigDecimal.ZERO);
        result.deliver();
        result.confirmByHost();
        when(recruitmentResultRepository.findByIdForUpdate(RESULT_ID))
                .thenReturn(Optional.of(result));
        when(recruitmentResultItemRepository.findAllByRecruitmentResultId(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.cancel(RESULT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_RESULT_NOT_CANCELABLE);
    }

    @Test
    void cancelSucceedsWhenGenerated() {
        RecruitmentResult result =
                RecruitmentResult.create(NOTICE_ID, HOST_CLIENT_ID, 0, 0, BigDecimal.ZERO);
        when(recruitmentResultRepository.findByIdForUpdate(RESULT_ID))
                .thenReturn(Optional.of(result));
        when(recruitmentResultItemRepository.findAllByRecruitmentResultId(any()))
                .thenReturn(List.of());

        var response = service.cancel(RESULT_ID);

        assertThat(response.status()).isEqualTo(RecruitmentResultStatus.CANCELED);
    }
}
