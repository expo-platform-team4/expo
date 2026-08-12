package com.expo.ticket.converter;

import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.dto.TicketProductSearchResponse;
import com.expo.ticket.dto.TicketUpdateResponse;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketProduct;
import org.springframework.stereotype.Component;

@Component
public class TicketProductConverter {

    public TicketProduct toEntity(Long expoId, TicketProductCreateRequest request) {
        return TicketProduct.create(
                expoId,
                request.name(),
                request.description(),
                request.price(),
                request.salesStartAt(),
                request.salesEndAt(),
                request.maxQuantityPerOrder());
    }

    public TicketProductCreateResponse toCreateResponse(TicketProduct product) {
        TicketInventory inventory = product.getInventory();
        int availableQuantity =
                inventory.getTotalQuantity()
                        - inventory.getReservedQuantity()
                        - inventory.getSoldQuantity();

        return new TicketProductCreateResponse(
                product.getId(),
                product.getExpoId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSalesStartAt(),
                product.getSalesEndAt(),
                inventory.getTotalQuantity(),
                availableQuantity,
                product.getMaxQuantityPerOrder(),
                product.getStatus());
    }

    public TicketProductSearchResponse toSearchTicketProduct(TicketProduct product) {
        TicketInventory inventory = product.getInventory();

        return new TicketProductSearchResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSalesStartAt(),
                product.getSalesEndAt(),
                inventory.getTotalQuantity(),
                inventory.getAvailableQuantity(),
                product.getMaxQuantityPerOrder(),
                product.getStatus());
    }

    public TicketUpdateResponse toUpdateTicket(TicketProduct product) {
        TicketInventory inventory = product.getInventory();

        int availableQuantity =
                inventory.getTotalQuantity()
                        - inventory.getReservedQuantity()
                        - inventory.getSoldQuantity();

        return new TicketUpdateResponse(
                product.getId(),
                product.getPrice(),
                inventory.getTotalQuantity(),
                availableQuantity,
                product.getUpdatedAt());
    }
}
