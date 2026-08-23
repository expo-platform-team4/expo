package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.converter.TicketOrderConverter;
import com.expo.ticket.dto.GuestTicketOrderReponse;
import com.expo.ticket.dto.GuestTicketOrderRequest;
import com.expo.ticket.dto.MemberTicketOrderRequest;
import com.expo.ticket.dto.TicketOrderItemRequest;
import com.expo.ticket.dto.TicketOrderResponse;
import com.expo.ticket.entity.GuestOrder;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderItem;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.GuestOrderInfoRepository;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketInventoryRepository;
import com.expo.ticket.repository.TicketOrderRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** {@link TicketOrderService}의 회원·비회원 주문 저장과 재고 예약을 확인한다. */
class TicketOrderServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 2L;
    private static final Long PRODUCT_ID = 3L;

    private TicketOrderItemCreatorService itemCreatorService;
    private TicketOrderRepository ticketOrderRepository;
    private TicketInventoryRepository ticketInventoryRepository;
    private PasswordEncoder passwordEncoder;
    private GuestOrderInfoRepository guestOrderInfoRepository;
    private TicketOrderConverter ticketOrderConverter;
    private TicketOrderService service;

    @BeforeEach
    void setUp() {
        itemCreatorService = mock(TicketOrderItemCreatorService.class);
        ticketOrderRepository = mock(TicketOrderRepository.class);
        ticketInventoryRepository = mock(TicketInventoryRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        guestOrderInfoRepository = mock(GuestOrderInfoRepository.class);
        ticketOrderConverter = mock(TicketOrderConverter.class);
        service =
                new TicketOrderService(
                        mock(InventoryReservationRepository.class),
                        ticketOrderConverter,
                        itemCreatorService,
                        ticketOrderRepository,
                        ticketInventoryRepository,
                        passwordEncoder,
                        guestOrderInfoRepository);
    }

    @Test
    void memberCreateOrderSavesPendingOrderAndReservesInventory() {
        TicketOrderItem item = item(2);
        TicketOrderResponse response = mock(TicketOrderResponse.class);
        when(itemCreatorService.creatorOrderItems(any())).thenReturn(List.of(item));
        when(ticketOrderRepository.save(any(TicketOrder.class)))
                .thenAnswer(
                        invocation -> {
                            TicketOrder order = invocation.getArgument(0);
                            setField(order, "id", ORDER_ID);
                            return order;
                        });
        when(ticketInventoryRepository.reserveIfAvailable(PRODUCT_ID, 2)).thenReturn(1);
        when(ticketOrderConverter.toTicketOrderResponse(any(TicketOrder.class), any(Instant.class)))
                .thenReturn(response);

        TicketOrderResponse result = service.memberCreateOrder(MEMBER_ID, memberRequest());

        assertThat(result).isSameAs(response);
        verify(ticketInventoryRepository).reserveIfAvailable(PRODUCT_ID, 2);
    }

    @Test
    void guestCreateOrderHashesLookupPasswordAndPersistsGuestInformation() {
        TicketOrderItem item = item(1);
        GuestTicketOrderReponse response = mock(GuestTicketOrderReponse.class);
        when(itemCreatorService.creatorOrderItems(any())).thenReturn(List.of(item));
        when(ticketOrderRepository.save(any(TicketOrder.class)))
                .thenAnswer(
                        invocation -> {
                            TicketOrder order = invocation.getArgument(0);
                            setField(order, "id", ORDER_ID);
                            return order;
                        });
        when(passwordEncoder.encode("lookup-password")).thenReturn("encoded-password");
        when(guestOrderInfoRepository.saveAndFlush(any(GuestOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketInventoryRepository.reserveIfAvailable(PRODUCT_ID, 1)).thenReturn(1);
        when(ticketOrderConverter.toGuestTicketOrderResponse(
                        any(TicketOrder.class), any(Instant.class), any(GuestOrder.class)))
                .thenReturn(response);

        GuestTicketOrderReponse result = service.guestCreateOrder(guestRequest());

        assertThat(result).isSameAs(response);
        verify(passwordEncoder).encode("lookup-password");
        verify(guestOrderInfoRepository).saveAndFlush(any(GuestOrder.class));
        verify(ticketInventoryRepository).reserveIfAvailable(PRODUCT_ID, 1);
    }

    @Test
    void memberCreateOrderRejectsWhenConditionalInventoryReservationFails() {
        when(itemCreatorService.creatorOrderItems(any())).thenReturn(List.of(item(1)));
        when(ticketOrderRepository.save(any(TicketOrder.class)))
                .thenAnswer(
                        invocation -> {
                            TicketOrder order = invocation.getArgument(0);
                            setField(order, "id", ORDER_ID);
                            return order;
                        });
        when(ticketInventoryRepository.reserveIfAvailable(PRODUCT_ID, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.memberCreateOrder(MEMBER_ID, memberRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_INSUFFICIENT_STOCK);
    }

    private MemberTicketOrderRequest memberRequest() {
        return new MemberTicketOrderRequest(List.of(new TicketOrderItemRequest(PRODUCT_ID, 1)));
    }

    private GuestTicketOrderRequest guestRequest() {
        return new GuestTicketOrderRequest(
                List.of(new TicketOrderItemRequest(PRODUCT_ID, 1)),
                "홍길동",
                "01012345678",
                20,
                "lookup-password");
    }

    private TicketOrderItem item(int quantity) {
        TicketProduct product =
                TicketProduct.create(
                        1L,
                        "1일권",
                        null,
                        BigDecimal.valueOf(10_000),
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        4);
        product.attachInventory(TicketInventory.create(product, 10));
        setField(product, "id", PRODUCT_ID);
        return TicketOrderItem.builder()
                .ticketProduct(product)
                .quantity(quantity)
                .unitPrice(BigDecimal.valueOf(10_000))
                .build();
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
