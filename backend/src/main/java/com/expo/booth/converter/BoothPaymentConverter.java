package com.expo.booth.converter;

import com.expo.booth.dto.BoothPaymentResponse;
import com.expo.booth.entity.BoothPayment;
import org.springframework.stereotype.Component;

@Component
public class BoothPaymentConverter {

    public BoothPaymentResponse toResponse(BoothPayment payment) {
        return new BoothPaymentResponse(
                payment.getId(),
                payment.getBoothOrderId(),
                payment.getPgOrderId(),
                payment.getPaymentKey(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getRequestedAmount(),
                payment.getApprovedAmount(),
                payment.getApprovedAt(),
                payment.getLastFailureCode(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
