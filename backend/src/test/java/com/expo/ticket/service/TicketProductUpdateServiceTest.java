package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketUpdateRequest;
import com.expo.ticket.dto.TicketUpdateResponse;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import com.expo.ticket.repository.TicketProductRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketProductUpdateService}의 수정 가능 상태·재고 수량 규칙을 확인한다. */
class TicketProductUpdateServiceTest {

    private static final Long EXPO_ID = 1L;
    private static final Long HOST_ID = 2L;
    private static final Long PRODUCT_ID = 3L;

    private TicketProductRepository ticketProductRepository;
    private ExpoRepository expoRepository;
    private TicketProductConverter ticketProductConverter;
    private TicketProductUpdateService service;

    @BeforeEach
    void setUp() {
        ticketProductRepository = mock(TicketProductRepository.class);
        expoRepository = mock(ExpoRepository.class);
        ticketProductConverter = mock(TicketProductConverter.class);
        service =
                new TicketProductUpdateService(
                        ticketProductRepository, expoRepository, ticketProductConverter);
    }

    @Test
    void ticketUpdateRejectsWhenProductDoesNotExistInExpo() {
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ticketUpdate(HOST_ID, EXPO_ID, PRODUCT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_NOT_FOUND);
    }

    @Test
    void ticketUpdateRejectsWhenProductIsAlreadyOnSale() {
        TicketProduct product = product(100);
        setField(product, "status", TicketProductStatus.ON_SALE);
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.ticketUpdate(HOST_ID, EXPO_ID, PRODUCT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_PRODUCT_UPDATE_NOT_ALLOWED);
    }

    @Test
    void ticketUpdateChangesDraftProductAndInventory() {
        TicketProduct product = product(100);
        TicketUpdateRequest request = new TicketUpdateRequest(BigDecimal.valueOf(12_000), 120);
        TicketUpdateResponse response = mock(TicketUpdateResponse.class);
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.of(product));
        when(ticketProductConverter.toUpdateTicket(product)).thenReturn(response);

        TicketUpdateResponse result = service.ticketUpdate(HOST_ID, EXPO_ID, PRODUCT_ID, request);

        assertThat(result).isSameAs(response);
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(product.getInventory().getTotalQuantity()).isEqualTo(120);
        verify(ticketProductRepository).saveAndFlush(product);
    }

    private void allowHost() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID)));
    }

    private TicketProduct product(int totalQuantity) {
        TicketProduct product =
                TicketProduct.create(
                        EXPO_ID,
                        "1일권",
                        null,
                        BigDecimal.valueOf(10_000),
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        2);
        product.attachInventory(TicketInventory.create(product, totalQuantity));
        return product;
    }

    private TicketUpdateRequest request() {
        return new TicketUpdateRequest(BigDecimal.valueOf(12_000), 120);
    }

    private Expo expo(Long hostClientId) {
        try {
            Constructor<Expo> constructor = Expo.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Expo expo = constructor.newInstance();
            setField(expo, "hostClientId", hostClientId);
            return expo;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
