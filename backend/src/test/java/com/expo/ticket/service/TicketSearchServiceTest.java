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
import com.expo.ticket.dto.TicketProductSearchResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.TicketProductRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketSearchService}의 주최자 전용 상품 조회 규칙을 확인한다. */
class TicketSearchServiceTest {

    private static final Long EXPO_ID = 1L;
    private static final Long HOST_ID = 2L;

    private TicketProductConverter ticketProductConverter;
    private ExpoRepository expoRepository;
    private TicketProductRepository ticketProductRepository;
    private TicketSearchService service;

    @BeforeEach
    void setUp() {
        ticketProductConverter = mock(TicketProductConverter.class);
        expoRepository = mock(ExpoRepository.class);
        ticketProductRepository = mock(TicketProductRepository.class);
        service =
                new TicketSearchService(
                        ticketProductConverter, expoRepository, ticketProductRepository);
    }

    @Test
    void ticketProductSearchRejectsWhenExpoDoesNotExist() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ticketProductSearch(HOST_ID, EXPO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPO_NOT_FOUND);
    }

    @Test
    void ticketProductSearchRejectsWhenRequesterIsNotExpoHost() {
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID + 1)));

        assertThatThrownBy(() -> service.ticketProductSearch(HOST_ID, EXPO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_EXPO_HOST);
    }

    @Test
    void ticketProductSearchConvertsEveryProductOwnedByRequester() {
        TicketProduct first = mock(TicketProduct.class);
        TicketProduct second = mock(TicketProduct.class);
        TicketProductSearchResponse firstResponse = mock(TicketProductSearchResponse.class);
        TicketProductSearchResponse secondResponse = mock(TicketProductSearchResponse.class);
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo(HOST_ID)));
        when(ticketProductRepository.findByExpoId(EXPO_ID)).thenReturn(List.of(first, second));
        when(ticketProductConverter.toSearchTicketProduct(first)).thenReturn(firstResponse);
        when(ticketProductConverter.toSearchTicketProduct(second)).thenReturn(secondResponse);

        List<TicketProductSearchResponse> result = service.ticketProductSearch(HOST_ID, EXPO_ID);

        assertThat(result).containsExactly(firstResponse, secondResponse);
        verify(ticketProductRepository).findByExpoId(EXPO_ID);
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
