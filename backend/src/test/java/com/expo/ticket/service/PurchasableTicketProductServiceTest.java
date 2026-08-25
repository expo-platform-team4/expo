package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.PurchasableTicketProductResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import com.expo.ticket.repository.TicketProductRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link PurchasableTicketProductService}의 공개 구매 가능 상품 조회를 확인한다. */
class PurchasableTicketProductServiceTest {

    private static final Long EXPO_ID = 1L;

    private TicketProductRepository ticketProductRepository;
    private ExpoRepository expoRepository;
    private TicketProductConverter ticketProductConverter;
    private PurchasableTicketProductService service;

    @BeforeEach
    void setUp() {
        ticketProductRepository = mock(TicketProductRepository.class);
        expoRepository = mock(ExpoRepository.class);
        ticketProductConverter = mock(TicketProductConverter.class);
        service =
                new PurchasableTicketProductService(
                        ticketProductRepository, expoRepository, ticketProductConverter);
    }

    @Test
    void purchasableTicketRejectsWhenExpoDoesNotExist() {
        when(expoRepository.existsById(EXPO_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.purchasableTicket(EXPO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPO_NOT_FOUND);
    }

    @Test
    void purchasableTicketReturnsOnlyRepositorySelectedProducts() {
        TicketProduct product = mock(TicketProduct.class);
        PurchasableTicketProductResponse response = mock(PurchasableTicketProductResponse.class);
        when(expoRepository.existsById(EXPO_ID)).thenReturn(true);
        when(ticketProductRepository.findPurchasableByExpoId(
                        eq(EXPO_ID), eq(TicketProductStatus.ON_SALE), any(Instant.class)))
                .thenReturn(List.of(product));
        when(ticketProductConverter.toPurchasableTicket(product)).thenReturn(response);

        List<PurchasableTicketProductResponse> result = service.purchasableTicket(EXPO_ID);

        assertThat(result).containsExactly(response);
        verify(ticketProductRepository)
                .findPurchasableByExpoId(
                        eq(EXPO_ID), eq(TicketProductStatus.ON_SALE), any(Instant.class));
    }
}
