package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.TicketProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TicketProductCreateService {

    private final TicketProductConverter ticketProductConverter;
    private final TicketProductRepository ticketProductRepository;
    private final ExpoRepository expoRepository;

    @Transactional
    public TicketProductCreateResponse ticketCreate(
            Long expoId, Long memberId, TicketProductCreateRequest request) {
        validateSalesPeriod(request);
        validateExpoOwner(memberId, expoId);

        TicketProduct product = ticketProductConverter.toEntity(expoId, request);
        TicketInventory inventory = TicketInventory.create(product, request.totalQuantity());
        product.attachInventory(inventory);

        TicketProduct savedProduct = ticketProductRepository.save(product);
        return ticketProductConverter.toCreateResponse(savedProduct);
    }

    private void validateSalesPeriod(TicketProductCreateRequest request) {
        if (!request.salesStartAt().isBefore(request.salesEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_SALES_PERIOD);
        }
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
