package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.dto.TicketOrderItemRequest;
import com.expo.ticket.entity.TicketOrderItem;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import com.expo.ticket.repository.TicketProductRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketOrderItemCreatorService {

    private final TicketProductRepository ticketProductRepository;

    public List<TicketOrderItem> creatorOrderItems(List<TicketOrderItemRequest> requests) {
        List<TicketOrderItem> orderItems = new ArrayList<>();
        Set<Long> ticketProductIds = new HashSet<>();
        Long expoId = null;

        for (TicketOrderItemRequest itemRequest : requests) {
            validateDuplicateTicketProduct(ticketProductIds, itemRequest.ticketProductId());

            TicketProduct product =
                    ticketProductRepository
                            .findById(itemRequest.ticketProductId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

            validateSameExpo(expoId, product.getExpoId());
            expoId = product.getExpoId();

            validatePurchasable(product, itemRequest.quantity());

            TicketOrderItem orderItem =
                    TicketOrderItem.builder()
                            .ticketProduct(product)
                            .quantity(itemRequest.quantity())
                            .unitPrice(product.getPrice())
                            .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void validateSameExpo(Long expectedExpoId, Long actualExpoId) {
        if (expectedExpoId != null && !expectedExpoId.equals(actualExpoId)) {
            throw new BusinessException(ErrorCode.TICKET_ORDER_MULTIPLE_EXPOS);
        }
    }

    private void validateDuplicateTicketProduct(Set<Long> ticketProductIds, Long ticketProductId) {
        if (!ticketProductIds.add(ticketProductId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TICKET_PRODUCT_IN_ORDER);
        }
    }

    private void validatePurchasable(TicketProduct product, int quantity) {
        Instant now = Instant.now();

        if (now.isBefore(product.getSalesStartAt()) || now.isAfter(product.getSalesEndAt())) {
            throw new BusinessException(ErrorCode.TICKET_PRODUCT_NOT_ON_SALE_PERIOD);
        }

        if (product.getStatus() != TicketProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.TICKET_PRODUCT_NOT_ON_SALE);
        }

        if (quantity > product.getMaxQuantityPerOrder()) {
            throw new BusinessException(ErrorCode.TICKET_QUANTITY_EXCEEDS_LIMIT);
        }

        int availableQuantity =
                product.getInventory().getTotalQuantity()
                        - product.getInventory().getReservedQuantity()
                        - product.getInventory().getSoldQuantity();

        if (quantity > availableQuantity) {
            throw new BusinessException(ErrorCode.TICKET_INSUFFICIENT_STOCK);
        }
    }
}
