package com.expo.booth.converter;

import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.PublicBoothContentResponse;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.ExternalLink;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BoothContentConverter {

    private final BoothContentFileConverter boothContentFileConverter;
    private final ExternalLinkConverter externalLinkConverter;

    public BoothContentConverter(
            BoothContentFileConverter boothContentFileConverter,
            ExternalLinkConverter externalLinkConverter) {
        this.boothContentFileConverter = boothContentFileConverter;
        this.externalLinkConverter = externalLinkConverter;
    }

    public BoothContentResponse toResponse(
            BoothContent content, List<BoothContentFile> files, List<ExternalLink> links) {
        return new BoothContentResponse(
                content.getId(),
                content.getBoothAllocationId(),
                content.getClientUserId(),
                content.getCompanyDisplayName(),
                content.getTitle(),
                content.getCompanyDescription(),
                content.getBoothDescription(),
                content.getProductDescription(),
                content.getLogoFileId(),
                content.getMainImageFileId(),
                content.getStatus(),
                content.getPublishedAt(),
                content.getCorrectionRequestedAt(),
                content.getCorrectionMessage(),
                content.getCheckedByAdminId(),
                content.getCheckedAt(),
                files.stream().map(boothContentFileConverter::toResponse).toList(),
                links.stream().map(externalLinkConverter::toResponse).toList(),
                content.getCreatedAt(),
                content.getUpdatedAt());
    }

    public PublicBoothContentResponse toPublicResponse(
            BoothContent content, List<BoothContentFile> files, List<ExternalLink> links) {
        return new PublicBoothContentResponse(
                content.getId(),
                content.getBoothAllocationId(),
                content.getCompanyDisplayName(),
                content.getTitle(),
                content.getCompanyDescription(),
                content.getBoothDescription(),
                content.getProductDescription(),
                content.getLogoFileId(),
                content.getMainImageFileId(),
                content.getPublishedAt(),
                files.stream().map(boothContentFileConverter::toResponse).toList(),
                links.stream().map(externalLinkConverter::toResponse).toList());
    }
}
