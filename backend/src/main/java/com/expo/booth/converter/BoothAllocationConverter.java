package com.expo.booth.converter;

import com.expo.booth.dto.BoothAllocationResponse;
import com.expo.booth.entity.BoothAllocation;
import org.springframework.stereotype.Component;

@Component
public class BoothAllocationConverter {

    public BoothAllocationResponse toResponse(BoothAllocation allocation) {
        return new BoothAllocationResponse(
                allocation.getId(),
                allocation.getApplicationId(),
                allocation.getBoothOrderId(),
                allocation.getBoothProductId(),
                allocation.getClientUserId(),
                allocation.getAllocatedAt(),
                allocation.getStatus(),
                allocation.getCanceledAt(),
                allocation.getCancelReason(),
                allocation.getCreatedAt(),
                allocation.getUpdatedAt());
    }
}
