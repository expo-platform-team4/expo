package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketProductSearchResponse;
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

/** {@link TicketProductStatusService}의 허용된 판매 상태 전환만 통과시키는 규칙을 확인한다. */
class TicketProductStatusServiceTest {

    private static final Long EXPO_ID = 1L;
    private static final Long HOST_ID = 2L;
    private static final Long PRODUCT_ID = 3L;
    private static final Long OTHER_MEMBER_ID = 99L;

    private TicketProductRepository ticketProductRepository;
    private ExpoRepository expoRepository;
    private TicketProductConverter ticketProductConverter;
    private TicketProductStatusService service;

    @BeforeEach
    void setUp() {
        ticketProductRepository = mock(TicketProductRepository.class);
        expoRepository = mock(ExpoRepository.class);
        ticketProductConverter = mock(TicketProductConverter.class);
        service =
                new TicketProductStatusService(
                        ticketProductRepository, expoRepository, ticketProductConverter);
    }

    @Test
    void changeStatusRejectsWhenNotExpoHost() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID)));

        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        OTHER_MEMBER_ID,
                                        EXPO_ID,
                                        PRODUCT_ID,
                                        TicketProductStatus.ON_SALE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_EXPO_HOST);
    }

    @Test
    void changeStatusRejectsWhenProductDoesNotExistInExpo() {
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        HOST_ID, EXPO_ID, PRODUCT_ID, TicketProductStatus.ON_SALE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_NOT_FOUND);
    }

    @Test
    void changeStatusAllowsDraftToOnSale() {
        TicketProduct product = product();
        TicketProductSearchResponse response = mock(TicketProductSearchResponse.class);
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.of(product));
        when(ticketProductConverter.toSearchTicketProduct(product)).thenReturn(response);

        TicketProductSearchResponse result =
                service.changeStatus(HOST_ID, EXPO_ID, PRODUCT_ID, TicketProductStatus.ON_SALE);

        assertThat(result).isSameAs(response);
        assertThat(product.getStatus()).isEqualTo(TicketProductStatus.ON_SALE);
    }

    @Test
    void changeStatusAllowsOnSaleToCanceled() {
        TicketProduct product = product();
        setField(product, "status", TicketProductStatus.ON_SALE);
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.of(product));
        when(ticketProductConverter.toSearchTicketProduct(product))
                .thenReturn(mock(TicketProductSearchResponse.class));

        service.changeStatus(HOST_ID, EXPO_ID, PRODUCT_ID, TicketProductStatus.CANCELED);

        assertThat(product.getStatus()).isEqualTo(TicketProductStatus.CANCELED);
    }

    @Test
    void changeStatusRejectsOnSaleToOnSale() {
        TicketProduct product = product();
        setField(product, "status", TicketProductStatus.ON_SALE);
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        HOST_ID, EXPO_ID, PRODUCT_ID, TicketProductStatus.ON_SALE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
    }

    @Test
    void changeStatusRejectsFromCanceled() {
        TicketProduct product = product();
        setField(product, "status", TicketProductStatus.CANCELED);
        allowHost();
        when(ticketProductRepository.findByIdAndExpoId(PRODUCT_ID, EXPO_ID))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(
                        () ->
                                service.changeStatus(
                                        HOST_ID, EXPO_ID, PRODUCT_ID, TicketProductStatus.ON_SALE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
    }

    private void allowHost() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID)));
    }

    private TicketProduct product() {
        TicketProduct product =
                TicketProduct.create(
                        EXPO_ID,
                        "1일권",
                        null,
                        BigDecimal.valueOf(10_000),
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        2);
        product.attachInventory(TicketInventory.create(product, 100));
        return product;
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
