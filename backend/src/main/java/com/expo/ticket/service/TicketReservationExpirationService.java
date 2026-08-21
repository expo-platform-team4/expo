package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.entity.InventoryReservation;
import com.expo.ticket.entity.InventoryReservationStatus;
import com.expo.ticket.repository.InventoryReservationRepository;
import com.expo.ticket.repository.TicketInventoryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 만료된 티켓 재고 예약을 해제하고 재고를 반환한다. */
@Service
@RequiredArgsConstructor
public class TicketReservationExpirationService {

    private final InventoryReservationRepository inventoryReservationRepository;
    private final TicketInventoryRepository ticketInventoryRepository;

    @Transactional
    public int expireReservations() {
        List<InventoryReservation> reservations =
                inventoryReservationRepository.findAllExpiredForUpdate(
                        InventoryReservationStatus.ACTIVE, Instant.now());
        for (InventoryReservation reservation : reservations) {
            int updated =
                    ticketInventoryRepository.releaseReserved(
                            reservation.getTicketProduct().getId(), reservation.getQuantity());
            if (updated != 1) {
                throw new BusinessException(ErrorCode.PAYMENT_RESERVATION_NOT_FOUND);
            }
            reservation.expire();
        }
        return reservations.size();
    }
}
