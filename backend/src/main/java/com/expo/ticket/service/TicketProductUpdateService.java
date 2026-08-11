package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketUpdateRequest;
import com.expo.ticket.dto.TicketUpdateResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.TicketProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TicketProductUpdateService {

    private final TicketProductRepository ticketProductRepository;
    private final ExpoRepository expoRepository;
    private final TicketProductConverter ticketProductConverter;

    @Transactional
    public TicketUpdateResponse ticketUpdate(
            Long memberId, Long expoId, Long ticketProductId, TicketUpdateRequest request) {
        validateExpoOwner(memberId, expoId);

        TicketProduct product =
                ticketProductRepository
                        .findByIdAndExpoId(ticketProductId, expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        product.updatePrice(request.price());
        product.getInventory().updateTotalQuantity(request.totalQuantity());

        ticketProductRepository.saveAndFlush(product);

        return ticketProductConverter.toUpdateTicket(product);
    }

    private void validateExpoOwner(Long memberId, Long expoId) {
        Expo expo =
                expoRepository
                        .findById(expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_NOT_FOUND));

        if (!expo.getHostClientId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_EXPO_HOST);
        }
    }
}
