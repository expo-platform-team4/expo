package com.expo.expo.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code v_public_expo_cards} 한 행을 그대로 받는 타입.
 *
 * <p>응답 DTO 를 매퍼가 직접 만들지 않는 이유는 응답에 <b>뷰에 없는 필드</b>(내려받기 경로)가 있기 때문이다. record 는 정규 생성자로
 * 매핑되므로 컬럼이 하나라도 모자라면 맞지 않는다. 경로를 SQL 이 만들게 하면 URL 규칙이 SQL 로 새어 나간다.
 */
public record PublicExpoCardRow(
        Long expoId,
        String title,
        Instant eventStartAt,
        Instant eventEndAt,
        String regionCode,
        BigDecimal minimumPrice,
        long availableQuantity,
        String displaySalesStatus,
        Long thumbnailFileId) {}
