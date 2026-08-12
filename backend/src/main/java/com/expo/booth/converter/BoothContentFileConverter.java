package com.expo.booth.converter;

import com.expo.booth.dto.BoothContentFileResponse;
import com.expo.booth.entity.BoothContentFile;
import org.springframework.stereotype.Component;

@Component
public class BoothContentFileConverter {

    public BoothContentFileResponse toResponse(BoothContentFile file) {
        return new BoothContentFileResponse(
                file.getId(),
                file.getFileId(),
                file.getFileType(),
                file.getTitle(),
                file.getSortOrder(),
                file.getCreatedAt());
    }
}
