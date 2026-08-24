package com.expo.expo.converter;

import com.expo.expo.dto.ExpoFileResponse;
import com.expo.expo.dto.ExpoImageResponse;
import com.expo.expo.entity.ExpoFile;
import com.expo.expo.entity.ExpoImage;
import com.expo.file.converter.FileConverter;
import org.springframework.stereotype.Component;

/** 박람회 이미지·자료 Entity → DTO. */
@Component
public class ExpoContentConverter {

    /**
     * @param originalFilename 제목이 비어 있을 때 대신 보여 줄 이름. 없으면 {@code null}
     */
    public ExpoFileResponse toResponse(ExpoFile file, String originalFilename) {
        String title = file.getTitle();
        return new ExpoFileResponse(
                file.getId(),
                file.getFileId(),
                file.getFilePurpose().name(),
                title == null || title.isBlank() ? originalFilename : title,
                file.getSortOrder(),
                FileConverter.downloadUrl(file.getFileId()));
    }

    public ExpoImageResponse toResponse(ExpoImage image) {
        return new ExpoImageResponse(
                image.getId(),
                image.getFileId(),
                image.getImageType().name(),
                image.getAltText(),
                image.getSortOrder(),
                FileConverter.downloadUrl(image.getFileId()));
    }
}
