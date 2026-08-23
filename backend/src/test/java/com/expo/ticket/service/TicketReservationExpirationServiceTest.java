package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketInventoryRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketReservationExpirationService}의 만료 예약 재고 반환을 확인한다. */
class TicketReservationExpirationServiceTest {

    private static final Long PRODUCT_ID = 1L;

    private InventoryReservationRepository reservationRepository;
    private TicketInventoryRepository inventoryRepository;
    private TicketReservationExpirationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(InventoryReservationRepository.class);
        inventoryRepository = mock(TicketInventoryRepository.class);
        service =
                new TicketReservationExpirationService(reservationRepository, inventoryRepository);
    }

    @Test
    void expireReservationsReturnsStockAndExpiresEachLockedReservation() {
        InventoryReservation reservation = reservation();
        when(reservationRepository.findAllExpiredForUpdate(
                        org.mockito.ArgumentMatchers.eq(InventoryReservationStatus.ACTIVE),
                        org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(reservation));
        when(inventoryRepository.releaseReserved(PRODUCT_ID, 2)).thenReturn(1);

        int expiredCount = service.expireReservations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.EXPIRED);
    }

    @Test
    void expireReservationsFailsWhenStockReturnWasNotApplied() {
        InventoryReservation reservation = reservation();
        when(reservationRepository.findAllExpiredForUpdate(
                        org.mockito.ArgumentMatchers.eq(InventoryReservationStatus.ACTIVE),
                        org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(reservation));
        when(inventoryRepository.releaseReserved(PRODUCT_ID, 2)).thenReturn(0);

        assertThatThrownBy(service::expireReservations)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.ACTIVE);
    }

    private InventoryReservation reservation() {
        TicketProduct product =
                TicketProduct.create(
                        1L,
                        "1일권",
                        null,
                        BigDecimal.TEN,
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        1);
        product.attachInventory(TicketInventory.create(product, 2));
        setField(product, "id", PRODUCT_ID);
        TicketOrder order =
                TicketOrder.creatGuestOrder(
                        "TICKET-test",
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        2);
        return InventoryReservation.builder()
                .ticketProduct(product)
                .ticketOrder(order)
                .quantity(2)
                .expiresAt(Instant.now().minusSeconds(1))
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
