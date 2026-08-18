package com.expo.booth.converter;

import com.expo.booth.dto.BoothProductResponse;
import com.expo.booth.entity.Booth;
import com.expo.booth.entity.BoothProduct;
import com.expo.venue.entity.VenueHall;
import com.expo.venue.entity.VenueZone;
import org.springframework.stereotype.Component;

@Component
public class BoothProductConverter {

    /** 부스·구역·홀 위치 정보 없이 변환한다. 위치 조회가 불가능한 예외적인 경로에서만 쓴다. */
    public BoothProductResponse toResponse(BoothProduct product) {
        return toResponse(product, null, null, null);
    }

    public BoothProductResponse toResponse(
            BoothProduct product, Booth booth, VenueZone zone, VenueHall hall) {
        return new BoothProductResponse(
                product.getId(),
                product.getRecruitmentNoticeId(),
                product.getBoothId(),
                booth != null ? booth.getBoothNumber() : null,
                hall != null ? hall.getId() : null,
                hall != null ? hall.getName() : null,
                zone != null ? zone.getId() : null,
                zone != null ? zone.getName() : null,
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
