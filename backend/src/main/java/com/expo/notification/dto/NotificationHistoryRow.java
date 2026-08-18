package com.expo.notification.dto;

import java.time.Instant;

/**
 * 매퍼가 돌려주는 알림 이력 한 줄. <b>DB 모양 그대로다.</b>
 *
 * <p>응답 DTO({@link NotificationHistoryResponse})와 따로 두는 이유는 <b>수신번호</b> 때문이다. 여기에는 원문이
 * 담기고, 응답으로 나갈 때 마스킹된다. 한 record 로 합치면 마스킹을 빠뜨린 채 내보내기 쉽다.
 *
 * @param attemptCount 발송 시도 횟수. 한 번도 시도하지 않았으면 0 ({@code CANCELED} 가 그렇다)
 * @param lastAttemptNo 마지막 시도 번호. 시도가 없으면 {@code null}
 */
public record NotificationHistoryRow(
        Long notificationId,
        Long recipientUserId,
        String recipientPhoneNumber,
        String channel,
        String templateCode,
        String referenceType,
        Long referenceId,
        String status,
        int retryCount,
        String lastError,
        Instant sentAt,
        Instant createdAt,
        int attemptCount,
        Integer lastAttemptNo,
        String lastAttemptStatus,
        String lastAttemptErrorCode,
        Instant lastAttemptAt) {}
