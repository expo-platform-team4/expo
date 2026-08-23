package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.TicketProductRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketProductCreateService}의 상품 등록 권한·판매 기간 규칙을 확인한다. */
class TicketProductCreateServiceTest {

    private static final Long EXPO_ID = 1L;
    private static final Long HOST_ID = 2L;

    private TicketProductConverter ticketProductConverter;
    private TicketProductRepository ticketProductRepository;
    private ExpoRepository expoRepository;
    private TicketProductCreateService service;

    @BeforeEach
    void setUp() {
        ticketProductConverter = mock(TicketProductConverter.class);
        ticketProductRepository = mock(TicketProductRepository.class);
        expoRepository = mock(ExpoRepository.class);
        service =
                new TicketProductCreateService(
                        ticketProductConverter, ticketProductRepository, expoRepository);
    }

    @Test
    void ticketCreateRejectsWhenSalesPeriodIsInvalid() {
        TicketProductCreateRequest request = request(Instant.now().plusSeconds(60), Instant.now());

        assertThatThrownBy(() -> service.ticketCreate(EXPO_ID, HOST_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SALES_PERIOD);

        verifyNoInteractions(expoRepository, ticketProductConverter, ticketProductRepository);
    }

    @Test
    void ticketCreateRejectsWhenExpoDoesNotExist() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ticketCreate(EXPO_ID, HOST_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPO_NOT_FOUND);
    }

    @Test
    void ticketCreateRejectsWhenRequesterIsNotExpoHost() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID + 1)));

        assertThatThrownBy(() -> service.ticketCreate(EXPO_ID, HOST_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_EXPO_HOST);
    }

    @Test
    void ticketCreateSavesProductWithInitialInventoryAndReturnsConvertedResponse() {
        TicketProductCreateRequest request = request();
        TicketProduct product =
                TicketProduct.create(
                        EXPO_ID,
                        "1일권",
                        "일반 입장권",
                        BigDecimal.valueOf(10_000),
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        2);
        TicketProductCreateResponse response = mock(TicketProductCreateResponse.class);
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID)));
        when(ticketProductConverter.toEntity(EXPO_ID, request)).thenReturn(product);
        when(ticketProductRepository.save(product)).thenReturn(product);
        when(ticketProductConverter.toCreateResponse(product)).thenReturn(response);

        TicketProductCreateResponse result = service.ticketCreate(EXPO_ID, HOST_ID, request);

        assertThat(result).isSameAs(response);
        assertThat(product.getInventory()).isNotNull();
        assertThat(product.getInventory().getTotalQuantity()).isEqualTo(100);
        verify(ticketProductRepository).save(product);
    }

    private TicketProductCreateRequest request() {
        return request(Instant.now(), Instant.now().plusSeconds(3600));
    }

    private TicketProductCreateRequest request(Instant salesStartAt, Instant salesEndAt) {
        return new TicketProductCreateRequest(
                "1일권", "일반 입장권", BigDecimal.valueOf(10_000), salesStartAt, salesEndAt, 100, 2);
    }

    private Expo expo(Long hostClientId) {
        try {
            Constructor<Expo> constructor = Expo.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Expo expo = constructor.newInstance();
            Field field = Expo.class.getDeclaredField("hostClientId");
            field.setAccessible(true);
            field.set(expo, hostClientId);
            return expo;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
