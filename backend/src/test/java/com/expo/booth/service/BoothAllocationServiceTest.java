package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.BoothManagementHistoryRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** {@link BoothAllocationService} 의 조회·취소 규칙을 확인한다. */
class BoothAllocationServiceTest {

    private static final Long ALLOCATION_ID = 1L;
    private static final Long CLIENT_USER_ID = 2L;
    private static final Long APPLICATION_ID = 3L;
    private static final Long ORDER_ID = 4L;
    private static final Long BOOTH_PRODUCT_ID = 5L;
    private static final Long ADMIN_ID = 6L;

    private BoothAllocationRepository boothAllocationRepository;
    private BoothContentRepository boothContentRepository;
    private BoothManagementHistoryRepository boothManagementHistoryRepository;
    private BoothAllocationService service;

    @BeforeEach
    void setUp() {
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        boothContentRepository = mock(BoothContentRepository.class);
        boothManagementHistoryRepository = mock(BoothManagementHistoryRepository.class);
        service =
                new BoothAllocationService(
                        boothAllocationRepository,
                        boothContentRepository,
                        boothManagementHistoryRepository,
                        new BoothAllocationConverter());
    }

    private BoothAllocation allocation() {
        return BoothAllocation.create(APPLICATION_ID, ORDER_ID, BOOTH_PRODUCT_ID, CLIENT_USER_ID);
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

    /** 재판매 보상 흐름이 없어 부스 상품 상태는 건드리지 않고 배정만 취소 기록으로 남겨야 한다. */
    @Test
    void cancelSucceedsWithoutTouchingBoothProduct() {
        BoothAllocation allocation = allocation();
        when(boothAllocationRepository.findByIdForUpdate(ALLOCATION_ID))
                .thenReturn(Optional.of(allocation));

        var response = service.cancel(ALLOCATION_ID, ADMIN_ID, "이중 배정 정정");

        assertThat(response.cancelReason()).isEqualTo("이중 배정 정정");
        assertThat(response.status().name()).isEqualTo("CANCELED");
        verify(boothManagementHistoryRepository).save(any());
    }
}
