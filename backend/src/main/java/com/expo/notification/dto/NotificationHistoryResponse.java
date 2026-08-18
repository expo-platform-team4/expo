package com.expo.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 관리자에게 내보내는 알림 이력 한 줄.
 *
 * <p><b>수신번호는 마스킹된다.</b> 관리자 화면이라도 목록에 번호를 그대로 늘어놓을 이유가 없다. 누구인지 가려야 할 때는
 * {@code recipientUserId} 와 {@code referenceId} 로 찾아간다.
 */
@Schema(description = "알림 이력")
public record NotificationHistoryResponse(
        @Schema(description = "알림 ID. 재발송할 때 쓴다", example = "412") Long notificationId,
        @Schema(description = "수신 회원 ID. 비회원이면 null", example = "37") Long recipientUserId,
        @Schema(description = "수신번호 (마스킹)", example = "010****0340") String recipientPhoneNumber,
        @Schema(description = "SMS / KAKAO / EMAIL", example = "SMS") String channel,
        @Schema(
                        description = "TICKET_ISSUED / REFUND_COMPLETED / EXPO_CANCELED",
                        example = "EXPO_CANCELED")
                String templateCode,
        @Schema(description = "참조 대상 종류. ORDER / EXPO", example = "EXPO") String referenceType,
        @Schema(description = "참조 대상 ID", example = "9") Long referenceId,
        @Schema(description = "PENDING / SENT / FAILED / RETRYING / CANCELED", example = "FAILED")
                String status,
        @Schema(description = "재발송 횟수", example = "1") int retryCount,
        @Schema(description = "마지막 오류. 성공이면 null") String lastError,
        @Schema(description = "발송 완료 시각. 미발송이면 null") Instant sentAt,
        @Schema(description = "알림이 만들어진 시각") Instant createdAt,
        @Schema(description = "발송 시도 횟수. 시도 자체가 없었으면 0", example = "2") int attemptCount,
        @Schema(description = "마지막 시도 번호. 시도가 없으면 null", example = "2") Integer lastAttemptNo,
        @Schema(description = "마지막 시도 결과. REQUESTED / SENT / DELIVERED / FAILED")
                String lastAttemptStatus,
        @Schema(description = "대행사가 준 오류 코드", example = "1026") String lastAttemptErrorCode,
        @Schema(description = "마지막 시도 시각") Instant lastAttemptAt,
        @Schema(description = "재발송할 수 있는 상태인가", example = "true") boolean retryable) {}
