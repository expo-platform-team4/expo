package com.expo.booth.converter;

import com.expo.booth.dto.ExternalLinkResponse;
import com.expo.booth.entity.ExternalLink;
import org.springframework.stereotype.Component;

@Component
public class ExternalLinkConverter {

    public ExternalLinkResponse toResponse(ExternalLink link) {
        return new ExternalLinkResponse(
                link.getId(),
                link.getLinkType(),
                link.getLabel(),
                link.getUrl(),
                link.getSortOrder());
    }
}
