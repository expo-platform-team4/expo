package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.PurchasableTicketProductResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import com.expo.ticket.repository.TicketProductRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PurchasableTicketProductService {

    private final TicketProductRepository ticketProductRepository;
    private final ExpoRepository expoRepository;
    private final TicketProductConverter ticketProductConverter;

    public List<PurchasableTicketProductResponse> purchasableTicket(Long expoId) {
        validateExpo(expoId);

        List<TicketProduct> products =
                ticketProductRepository.findPurchasableByExpoId(
                        expoId, TicketProductStatus.ON_SALE, Instant.now());

        List<PurchasableTicketProductResponse> responses = new ArrayList<>();

        for (TicketProduct product : products) {
            PurchasableTicketProductResponse response =
                    ticketProductConverter.toPurchasableTicket(product);

            responses.add(response);
        }

        return responses;
    }

    private void validateExpo(Long expoId) {
        if (!expoRepository.existsById(expoId)) {
            throw new BusinessException(ErrorCode.EXPO_NOT_FOUND);
        }
    }
}
