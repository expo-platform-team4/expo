package com.expo.booth.converter;

import com.expo.booth.dto.BoothProductResponse;
import com.expo.booth.entity.BoothProduct;
import org.springframework.stereotype.Component;

@Component
public class BoothProductConverter {

    public BoothProductResponse toResponse(BoothProduct product) {
        return new BoothProductResponse(
                product.getId(),
                product.getRecruitmentNoticeId(),
                product.getBoothId(),
                product.getSupplyPrice(),
                product.getVatAmount(),
                product.getTotalPrice(),
                product.isVatIncluded(),
                product.getIncludedItems(),
                product.getSalesStartAt(),
                product.getSalesEndAt(),
                product.isPaymentEnabled(),
                product.getSalesStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
