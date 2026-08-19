package com.expo.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재발송 결과.
 *
 * <p>HTTP 는 <b>200</b> 이다. 발송이 실패해도 "재발송 요청을 처리했다" 는 사실은 성공이고, 실패는
 * {@code success=false} 로 알린다. 대행사가 거절한 것을 4xx·5xx 로 돌려주면 관리자 화면이 "요청이
 * 잘못됐다" 와 "보냈는데 거절당했다" 를 구분하지 못한다.
 *
 * @param attemptNo 이번 시도 번호. {@code message_histories} 에 이 번호로 한 행이 남는다
 */
@Schema(description = "알림 재발송 결과")
public record NotificationRetryResult(
        @Schema(description = "알림 ID", example = "412") Long notificationId,
        @Schema(description = "이번 시도 번호", example = "2") int attemptNo,
        @Schema(description = "재발송 뒤의 알림 상태. SENT 또는 FAILED", example = "SENT") String status,
        @Schema(description = "대행사가 접수했는가", example = "true") boolean success,
        @Schema(description = "실패 시 대행사 오류 코드. 성공이면 null", example = "1026") String errorCode) {}
