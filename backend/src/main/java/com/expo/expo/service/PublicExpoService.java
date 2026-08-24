package com.expo.expo.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.converter.PublicExpoConverter;
import com.expo.expo.dto.PublicExpoCardResponse;
import com.expo.expo.dto.PublicExpoDetailResponse;
import com.expo.expo.dto.PublicExpoDetailRow;
import com.expo.expo.dto.PublicExpoQuery;
import com.expo.expo.repository.PublicExpoMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비회원도 볼 수 있는 박람회 목록·상세 (이슈 #107).
 *
 * <p>{@code v_public_expo_cards} 뷰는 V1 부터 있었지만 읽는 코드가 없어 목록 화면이 "준비 중" 이었다. 박람회 이미지를 붙여도 보여 줄
 * 자리가 없어서 함께 만든다.
 */
@Service
@RequiredArgsConstructor
public class PublicExpoService {

    private final PublicExpoMapper publicExpoMapper;
    private final ExpoContentService expoContentService;
    private final PublicExpoConverter converter;

    @Transactional(readOnly = true)
    public List<PublicExpoCardResponse> listCards(PublicExpoQuery query) {
        return publicExpoMapper.findCards(query).stream().map(converter::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PublicExpoDetailResponse getDetail(Long expoId) {
        PublicExpoDetailRow row =
                publicExpoMapper
                        .findDetail(expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_NOT_FOUND));
        return converter.toResponse(
                row, expoContentService.listImages(expoId), expoContentService.listFiles(expoId));
    }
}
