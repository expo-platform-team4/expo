package com.expo.expo.converter;

import com.expo.expo.dto.ExpoFileResponse;
import com.expo.expo.dto.ExpoImageResponse;
import com.expo.expo.dto.PublicExpoCardResponse;
import com.expo.expo.dto.PublicExpoCardRow;
import com.expo.expo.dto.PublicExpoDetailResponse;
import com.expo.expo.dto.PublicExpoDetailRow;
import com.expo.file.converter.FileConverter;
import java.util.List;
import org.springframework.stereotype.Component;

/** 공개 박람회 뷰 행 → 응답 DTO. 내려받기 경로를 붙이는 자리다. */
@Component
public class PublicExpoConverter {

    public PublicExpoCardResponse toResponse(PublicExpoCardRow row) {
        return new PublicExpoCardResponse(
                row.expoId(),
                row.title(),
                row.eventStartAt(),
                row.eventEndAt(),
                row.regionCode(),
                row.minimumPrice(),
                row.availableQuantity(),
                row.displaySalesStatus(),
                row.thumbnailFileId() == null
                        ? null
                        : FileConverter.downloadUrl(row.thumbnailFileId()));
    }

    public PublicExpoDetailResponse toResponse(
            PublicExpoDetailRow row, List<ExpoImageResponse> images, List<ExpoFileResponse> files) {
        return new PublicExpoDetailResponse(
                row.expoId(),
                row.title(),
                row.description(),
                row.regionCode(),
                row.eventStartAt(),
                row.eventEndAt(),
                row.salesStartAt(),
                row.salesEndAt(),
                row.eventStatus(),
                row.displaySalesStatus(),
                row.minimumPrice(),
                row.availableQuantity(),
                images,
                files);
    }
}
