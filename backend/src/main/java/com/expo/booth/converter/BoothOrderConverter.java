package com.expo.booth.converter;

import com.expo.booth.dto.BoothOrderResponse;
import com.expo.booth.entity.BoothOrder;
import org.springframework.stereotype.Component;

@Component
public class BoothOrderConverter {

    public BoothOrderResponse toResponse(BoothOrder order) {
        return new BoothOrderResponse(
                order.getId(),
                order.getApplicationId(),
                order.getClientUserId(),
                order.getBoothProductId(),
                order.getOrderNumber(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
