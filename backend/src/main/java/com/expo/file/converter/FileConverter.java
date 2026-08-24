package com.expo.file.converter;

import com.expo.file.dto.FileMetadataResponse;
import com.expo.file.entity.FileMetadata;
import org.springframework.stereotype.Component;

/** {@link FileMetadata} → 응답 DTO. */
@Component
public class FileConverter {

    public FileMetadataResponse toResponse(FileMetadata metadata) {
        return new FileMetadataResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getContentType(),
                metadata.getFileSize(),
                metadata.getAccessLevel().name(),
                downloadUrl(metadata.getId()));
    }

    /** 프론트가 {@code <img src>} 나 링크에 그대로 넣는 경로. 프록시를 타므로 호스트를 붙이지 않는다. */
    public static String downloadUrl(Long fileId) {
        return "/api/files/" + fileId + "/content";
    }
}
