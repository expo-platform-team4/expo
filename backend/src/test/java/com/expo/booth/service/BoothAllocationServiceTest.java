package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link BoothAllocationService} 의 조회·취소 규칙을 확인한다. */
class BoothAllocationServiceTest {

    private static final Long ALLOCATION_ID = 1L;
    private static final Long CLIENT_USER_ID = 2L;
    private static final Long APPLICATION_ID = 3L;
    private static final Long ORDER_ID = 4L;
    private static final Long BOOTH_PRODUCT_ID = 5L;

    private BoothAllocationRepository boothAllocationRepository;
    private BoothAllocationService service;

    @BeforeEach
    void setUp() {
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        service =
                new BoothAllocationService(
                        boothAllocationRepository, new BoothAllocationConverter());
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
    void cancelRejectsWhenNotFound() {
        when(boothAllocationRepository.findById(ALLOCATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(ALLOCATION_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND);
    }

    @Test
    void cancelRejectsWhenAlreadyCanceled() {
        BoothAllocation canceled = allocation();
        canceled.cancel("이전 취소");
        when(boothAllocationRepository.findById(ALLOCATION_ID)).thenReturn(Optional.of(canceled));

        assertThatThrownBy(() -> service.cancel(ALLOCATION_ID, "이중 배정 정정"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_CANCELABLE);
    }

    /** 재판매 보상 흐름이 없어 부스 상품 상태는 건드리지 않고 배정만 취소 기록으로 남겨야 한다. */
    @Test
    void cancelSucceedsWithoutTouchingBoothProduct() {
        BoothAllocation allocation = allocation();
        when(boothAllocationRepository.findById(ALLOCATION_ID)).thenReturn(Optional.of(allocation));

        var response = service.cancel(ALLOCATION_ID, "이중 배정 정정");

        assertThat(response.cancelReason()).isEqualTo("이중 배정 정정");
        assertThat(response.status().name()).isEqualTo("CANCELED");
    }
}
