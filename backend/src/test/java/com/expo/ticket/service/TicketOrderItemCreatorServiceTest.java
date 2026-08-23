package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.dto.TicketOrderItemRequest;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketOrderItem;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import com.expo.ticket.repository.TicketProductRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 회원·비회원 주문이 공통으로 사용하는 주문 항목 생성 규칙을 확인한다. */
class TicketOrderItemCreatorServiceTest {

    private static final Long PRODUCT_ID = 1L;

    private TicketProductRepository ticketProductRepository;
    private TicketOrderItemCreatorService service;

    @BeforeEach
    void setUp() {
        ticketProductRepository = mock(TicketProductRepository.class);
        service = new TicketOrderItemCreatorService(ticketProductRepository);
    }

    @Test
    void creatorOrderItemsRejectsDuplicateProduct() {
        List<TicketOrderItemRequest> requests =
                List.of(request(PRODUCT_ID, 1), request(PRODUCT_ID, 1));
        when(ticketProductRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(purchasableProduct(3)));

        assertThatThrownBy(() -> service.creatorOrderItems(requests))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_TICKET_PRODUCT_IN_ORDER);
    }

    @Test
    void creatorOrderItemsRejectsProductOutsideSalesPeriod() {
        TicketProduct product = purchasableProduct(3);
        setField(product, "salesEndAt", Instant.now().minusSeconds(1));
        when(ticketProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.creatorOrderItems(List.of(request(PRODUCT_ID, 1))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_PRODUCT_NOT_ON_SALE_PERIOD);
    }

    @Test
    void creatorOrderItemsRejectsUnavailableProductAndInsufficientStock() {
        TicketProduct notOnSale = purchasableProduct(3);
        setField(notOnSale, "status", TicketProductStatus.DRAFT);
        when(ticketProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(notOnSale));

        assertThatThrownBy(() -> service.creatorOrderItems(List.of(request(PRODUCT_ID, 1))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_PRODUCT_NOT_ON_SALE);

        TicketProduct product = purchasableProduct(1);
        when(ticketProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.creatorOrderItems(List.of(request(PRODUCT_ID, 2))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_INSUFFICIENT_STOCK);
    }

    @Test
    void creatorOrderItemsKeepsPriceAndQuantityAtOrderTime() {
        TicketProduct product = purchasableProduct(3);
        when(ticketProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        List<TicketOrderItem> items = service.creatorOrderItems(List.of(request(PRODUCT_ID, 2)));

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getUnitPrice())
                .isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(items.getFirst().getItemSubtotalAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(20_000));
    }

    private TicketOrderItemRequest request(Long productId, int quantity) {
        return new TicketOrderItemRequest(productId, quantity);
    }

    private TicketProduct purchasableProduct(int totalQuantity) {
        TicketProduct product =
                TicketProduct.create(
                        1L,
                        "1일권",
                        null,
                        BigDecimal.valueOf(10_000),
                        Instant.now().minusSeconds(60),
                        Instant.now().plusSeconds(3600),
                        4);
        product.attachInventory(TicketInventory.create(product, totalQuantity));
        setField(product, "status", TicketProductStatus.ON_SALE);
        return product;
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
