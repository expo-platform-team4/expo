package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentStatus;
import com.expo.booth.entity.BoothManagementActionType;
import com.expo.booth.entity.BoothManagementHistory;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.BoothManagementHistoryRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.entity.ParticipationApplicationStatus;
import com.expo.participation.repository.ParticipationApplicationRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** {@link BoothAllocationService} 의 조회·취소·재배정 규칙을 확인한다. */
class BoothAllocationServiceTest {

    private static final Long ALLOCATION_ID = 1L;
    private static final Long CLIENT_USER_ID = 2L;
    private static final Long APPLICATION_ID = 3L;
    private static final Long ORDER_ID = 4L;
    private static final Long BOOTH_PRODUCT_ID = 5L;
    private static final Long ADMIN_ID = 6L;
    private static final Long NEW_BOOTH_PRODUCT_ID = 7L;
    private static final Long NOTICE_ID = 8L;
    private static final Long OTHER_NOTICE_ID = 10L;
    private static final Long BOOTH_ID = 9L;

    private BoothAllocationRepository boothAllocationRepository;
    private BoothContentRepository boothContentRepository;
    private BoothProductRepository boothProductRepository;
    private ParticipationApplicationRepository participationApplicationRepository;
    private BoothManagementHistoryRepository boothManagementHistoryRepository;
    private BoothAllocationService service;

    @BeforeEach
    void setUp() {
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        boothContentRepository = mock(BoothContentRepository.class);
        boothProductRepository = mock(BoothProductRepository.class);
        participationApplicationRepository = mock(ParticipationApplicationRepository.class);
        boothManagementHistoryRepository = mock(BoothManagementHistoryRepository.class);
        service =
                new BoothAllocationService(
                        boothAllocationRepository,
                        boothContentRepository,
                        boothProductRepository,
                        participationApplicationRepository,
                        boothManagementHistoryRepository,
                        new BoothAllocationConverter());
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(submittedApplication()));
    }

    private BoothAllocation allocation() {
        return BoothAllocation.create(APPLICATION_ID, ORDER_ID, BOOTH_PRODUCT_ID, CLIENT_USER_ID);
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

    private BoothProduct product(BoothSalesStatus status) {
        return productForNotice(NOTICE_ID, status);
    }

    private BoothProduct productForNotice(Long noticeId, BoothSalesStatus status) {
        BoothProduct product =
                BoothProduct.create(
                        noticeId, BOOTH_ID, BigDecimal.valueOf(100000), null, true, null);
        if (status != BoothSalesStatus.AVAILABLE) {
            product.changeSalesStatus(status);
        }
        return product;
    }

    private ParticipationApplication submittedApplication() {
        ParticipationApplication application =
                ParticipationApplication.create(
                        NOTICE_ID, CLIENT_USER_ID, "회사", null, null, BOOTH_PRODUCT_ID);
        application.startPayment(ORDER_ID);
        application.submit();
        return application;
    }

    @Test
    void getMineRejectsWhenNotFound() {
        when(boothAllocationRepository.findByIdAndClientUserId(ALLOCATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(ALLOCATION_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND);
    }

    @Test
    void getMineSucceeds() {
        when(boothAllocationRepository.findByIdAndClientUserId(ALLOCATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(allocation()));

        var response = service.getMine(ALLOCATION_ID, CLIENT_USER_ID);

        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void listForAdminConvertsPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BoothAllocation> page = new PageImpl<>(List.of(allocation()), pageable, 1);
        when(boothAllocationRepository.findAll(pageable)).thenReturn(page);

        Page<?> response = service.listForAdmin(pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getForAdminRejectsWhenNotFound() {
        when(boothAllocationRepository.findById(ALLOCATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForAdmin(ALLOCATION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND);
    }

    @Test
    void getForAdminSucceeds() {
        when(boothAllocationRepository.findById(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation()));

        var response = service.getForAdmin(ALLOCATION_ID);

        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void cancelRejectsWhenNotFound() {
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND);
    }

    @Test
    void cancelRejectsWhenAlreadyCanceled() {
        BoothAllocation canceled = allocation();
        canceled.cancel("이전 취소");
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(canceled));

        assertThatThrownBy(() -> service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_CANCELABLE);
    }

    @Test
    void cancelRejectsWhenApplicationNotFound() {
        BoothAllocation allocation = allocation();
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation));
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPATION_APPLICATION_NOT_FOUND);
    }

    /**
     * 재판매 보상 흐름이 없어 부스 상품 상태는 건드리지 않고 배정만 취소 기록으로 남겨야 한다. 딸린 참여
     * 신청서는 실제로 없는 부스를 확정된 것처럼 보여주지 않도록 같이 취소돼야 한다.
     */
    @Test
    void cancelSucceedsWithoutTouchingBoothProduct() {
        BoothAllocation allocation = allocation();
        ParticipationApplication application = submittedApplication();
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation));
        when(participationApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        var response = service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정");

        assertThat(response.cancelReason()).isEqualTo("이중 배정 정정");
        assertThat(response.status().name()).isEqualTo("CANCELED");
        assertThat(application.getStatus()).isEqualTo(ParticipationApplicationStatus.CANCELED);
        verify(boothManagementHistoryRepository)
                .save(
                        argThat(
                                (BoothManagementHistory history) ->
                                        history.getActionType()
                                                        == BoothManagementActionType
                                                                .ALLOCATION_CORRECTED
                                                && ALLOCATION_ID.equals(
                                                        history.getBoothAllocationId())
                                                && "이중 배정 정정".equals(history.getReason())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }

    /** 공개 상태인 콘텐츠는 배정 취소 시 같이 숨겨져야 한다 - 안 그러면 공개 사이트에 계속 떠 있게 된다. */
    @Test
    void cancelHidesPublishedContent() {
        BoothAllocation allocation = allocation();
        BoothContent content =
                BoothContent.create(ALLOCATION_ID, CLIENT_USER_ID, "회사", "제목", null, null, null);
        withId(content, 10L);
        content.submitForReview();
        content.approve(ADMIN_ID);
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation));
        when(boothContentRepository.findByBoothAllocationId(ALLOCATION_ID))
                .thenReturn(Optional.of(content));

        service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정");

        assertThat(content.getStatus()).isEqualTo(BoothContentStatus.HIDDEN);
        verify(boothManagementHistoryRepository)
                .save(
                        argThat(
                                (BoothManagementHistory history) ->
                                        history.getActionType()
                                                        == BoothManagementActionType.CONTENT_HIDDEN
                                                && Long.valueOf(10L)
                                                        .equals(history.getBoothContentId())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }

    /** 아직 공개 전(DRAFT 등)인 콘텐츠는 배정을 취소해도 숨김 처리할 게 없다. */
    @Test
    void cancelDoesNotHideUnpublishedContent() {
        BoothAllocation allocation = allocation();
        BoothContent content =
                BoothContent.create(ALLOCATION_ID, CLIENT_USER_ID, "회사", "제목", null, null, null);
        withId(content, 10L);
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation));
        when(boothContentRepository.findByBoothAllocationId(ALLOCATION_ID))
                .thenReturn(Optional.of(content));

        service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정");

        assertThat(content.getStatus()).isEqualTo(BoothContentStatus.DRAFT);
        verify(boothManagementHistoryRepository, never())
                .save(
                        argThat(
                                (BoothManagementHistory history) ->
                                        history.getActionType()
                                                == BoothManagementActionType.CONTENT_HIDDEN));
    }

    @Test
    void reassignRejectsWhenAllocationNotFound() {
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.reassign(
                                        ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND);
    }

    @Test
    void reassignRejectsWhenAllocationNotAssigned() {
        BoothAllocation canceled = allocation();
        canceled.cancel("이전 취소");
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(canceled));

        assertThatThrownBy(
                        () ->
                                service.reassign(
                                        ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_REASSIGNABLE);
    }

    @Test
    void reassignRejectsWhenNewProductNotFound() {
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation()));
        when(boothProductRepository.findById(NEW_BOOTH_PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.reassign(
                                        ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_FOUND);
    }

    @Test
    void reassignRejectsWhenNewProductNotAvailable() {
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation()));
        when(boothProductRepository.findById(NEW_BOOTH_PRODUCT_ID))
                .thenReturn(Optional.of(product(BoothSalesStatus.SOLD)));

        assertThatThrownBy(
                        () ->
                                service.reassign(
                                        ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
    }

    /** 동시에 같은 상품으로 재배정하려는 요청은 사전 검사를 통과해도 DB 유니크 제약으로 최종 차단돼야 한다. */
    @Test
    void reassignTranslatesUniqueConstraintViolationToProductNotAvailable() {
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation()));
        when(boothProductRepository.findById(NEW_BOOTH_PRODUCT_ID))
                .thenReturn(Optional.of(product(BoothSalesStatus.AVAILABLE)));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID))
                .thenReturn(Optional.of(product(BoothSalesStatus.SOLD)));
        when(boothAllocationRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint"
                                        + " \"booth_allocations_booth_product_id_key\""));

        assertThatThrownBy(
                        () ->
                                service.reassign(
                                        ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_PRODUCT_NOT_AVAILABLE);
    }

    /** 다른 모집공고 소속 부스 상품으로는 재배정할 수 없어야 한다. */
    @Test
    void reassignRejectsWhenNewProductBelongsToDifferentNotice() {
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation()));
        when(boothProductRepository.findById(NEW_BOOTH_PRODUCT_ID))
                .thenReturn(
                        Optional.of(productForNotice(OTHER_NOTICE_ID, BoothSalesStatus.AVAILABLE)));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID))
                .thenReturn(Optional.of(product(BoothSalesStatus.SOLD)));

        assertThatThrownBy(
                        () ->
                                service.reassign(
                                        ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_REASSIGN_NOTICE_MISMATCH);
        verify(boothAllocationRepository, never()).saveAndFlush(any());
    }

    @Test
    void reassignSucceeds() {
        BoothAllocation allocation = allocation();
        BoothProduct previousProduct = product(BoothSalesStatus.SOLD);
        BoothProduct newProduct = product(BoothSalesStatus.AVAILABLE);
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation));
        when(boothProductRepository.findById(NEW_BOOTH_PRODUCT_ID))
                .thenReturn(Optional.of(newProduct));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID))
                .thenReturn(Optional.of(previousProduct));

        var response = service.reassign(ALLOCATION_ID, NEW_BOOTH_PRODUCT_ID, ADMIN_ID, "이중 배정 정정");

        assertThat(response.boothProductId()).isEqualTo(NEW_BOOTH_PRODUCT_ID);
        assertThat(previousProduct.getSalesStatus()).isEqualTo(BoothSalesStatus.AVAILABLE);
        assertThat(newProduct.getSalesStatus()).isEqualTo(BoothSalesStatus.SOLD);
        verify(boothManagementHistoryRepository)
                .save(
                        argThat(
                                (BoothManagementHistory history) ->
                                        history.getActionType()
                                                        == BoothManagementActionType
                                                                .INFORMATION_UPDATED
                                                && ALLOCATION_ID.equals(
                                                        history.getBoothAllocationId())
                                                && "이중 배정 정정".equals(history.getReason())
                                                && ADMIN_ID.equals(
                                                        history.getProcessedByAdminId())));
    }
}
