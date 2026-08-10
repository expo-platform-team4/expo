package com.expo.booth.converter;

import com.expo.booth.dto.BoothTemplateResponse;
import com.expo.booth.entity.BoothTemplate;
import org.springframework.stereotype.Component;

@Component
public class BoothTemplateConverter {

    public BoothTemplateResponse toResponse(BoothTemplate template) {
        return new BoothTemplateResponse(
                template.getId(),
                template.getShapeCode(),
                template.getName(),
                template.getWidth(),
                template.getHeight(),
                template.getDepth(),
                template.getDimensionUnit(),
                template.getDefaultIncludedItems(),
                template.getOperationalStatus(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
