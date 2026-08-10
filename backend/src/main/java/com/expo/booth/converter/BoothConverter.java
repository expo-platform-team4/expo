package com.expo.booth.converter;

import com.expo.booth.dto.BoothResponse;
import com.expo.booth.entity.Booth;
import org.springframework.stereotype.Component;

@Component
public class BoothConverter {

    public BoothResponse toResponse(Booth booth) {
        return new BoothResponse(
                booth.getId(),
                booth.getVenueZoneId(),
                booth.getBoothTemplateId(),
                booth.getBoothNumber(),
                booth.getShapeCode(),
                booth.getWidth(),
                booth.getHeight(),
                booth.getDepth(),
                booth.getDimensionUnit(),
                booth.getPositionX(),
                booth.getPositionY(),
                booth.getRotationDegree(),
                booth.getSortOrder(),
                booth.getOperationalStatus(),
                booth.getCreatedAt(),
                booth.getUpdatedAt());
    }
}
