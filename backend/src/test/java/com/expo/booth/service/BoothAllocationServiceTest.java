package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothAllocationConverter;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.math.BigDecimal;
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
    private static final Long NOTICE_ID = 6L;

    private BoothAllocationRepository boothAllocationRepository;
    private BoothProductRepository boothProductRepository;
    private BoothAllocationService service;

    @BeforeEach
    void setUp() {
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        boothProductRepository = mock(BoothProductRepository.class);
        service =
                new BoothAllocationService(
                        boothAllocationRepository,
                        boothProductRepository,
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

    @Test
    void cancelSucceedsAndRevertsProductToAvailable() {
        BoothAllocation allocation = allocation();
        BoothProduct product =
                BoothProduct.create(
                                NOTICE_ID,
                                10L,
                                BigDecimal.valueOf(1_000_000),
                                BigDecimal.valueOf(100_000),
                                true,
                                null)
                        .schedule(null, null, true);
        product.reserve();
        product.markSold();
        when(boothAllocationRepository.findById(ALLOCATION_ID)).thenReturn(Optional.of(allocation));
        when(boothProductRepository.findById(BOOTH_PRODUCT_ID)).thenReturn(Optional.of(product));

        var response = service.cancel(ALLOCATION_ID, "이중 배정 정정");

        assertThat(response.cancelReason()).isEqualTo("이중 배정 정정");
        assertThat(product.getSalesStatus()).isEqualTo(BoothSalesStatus.AVAILABLE);
    }
}
