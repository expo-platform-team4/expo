package com.expo.notification.controller;

import com.expo.common.response.ApiResponse;
import com.expo.notification.dto.NotificationHistoryPage;
import com.expo.notification.dto.NotificationRetryResult;
import com.expo.notification.service.NotificationHistoryService;
import com.expo.notification.service.NotificationRetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 알림 이력 조회 (D-API-009).
 *
 * <p>인가는 {@code SecurityConfig} 가 경로로 건다 — {@code /api/admin/**} 는 {@code ROLE_ADMIN} 이다.
 * 메서드마다 {@code @PreAuthorize} 를 붙이지 않는 것이 이 저장소의 방식이다.
 *
 * <h2>경로에 채널 이름을 넣지 않는다</h2>
 *
 * 처음 WBS 는 {@code kakao-notification-logs} 였다. 카카오 전용이던 시절 이름인데, 채널이 SMS 로
 * 바뀌면서 한 번 어긋났다. 채널을 이름에 박으면 이메일·알림톡이 붙을 때 또 어긋난다.
 *
 * <p><b>"logs" 도 쓰지 않는다.</b> 돌려주는 것이 로그가 아니라 <b>알림</b>이기 때문이다 —
 * {@code notifications} 1행에 {@code message_histories} 의 시도 요약을 붙인 모양이다. 재발송도
 * 시도 하나를 다시 보내는 것이 아니라 알림을 다시 보내는 것이라, 경로 변수도 {@code notificationId} 다.
 */
@Tag(name = "Admin Notification", description = "관리자 알림 이력 조회·재발송")
@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationHistoryService notificationHistoryService;
    private final NotificationRetryService notificationRetryService;

    public AdminNotificationController(
            NotificationHistoryService notificationHistoryService,
            NotificationRetryService notificationRetryService) {
        this.notificationHistoryService = notificationHistoryService;
        this.notificationRetryService = notificationRetryService;
    }

    @Operation(
            summary = "알림 이력 조회",
            description =
                    "발권·환불·박람회 취소 알림의 발송 이력을 조회합니다. 상태·템플릿·참조 대상·기간으로 "
                            + "필터링할 수 있습니다. 수신번호는 마스킹되어 나갑니다. "
                            + "page·size를 안 주면 첫 페이지(0번, 20건)만 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationHistoryPage>> searchNotifications(
            @Parameter(description = "PENDING / SENT / FAILED / RETRYING / CANCELED")
                    @RequestParam(required = false)
                    String status,
            @Parameter(description = "TICKET_ISSUED / REFUND_COMPLETED / EXPO_CANCELED")
                    @RequestParam(required = false)
                    String templateCode,
            @Parameter(description = "참조 대상 종류. ORDER / EXPO") @RequestParam(required = false)
                    String referenceType,
            @Parameter(description = "참조 대상 ID") @RequestParam(required = false) Long referenceId,
            @Parameter(description = "조회 시작 시각")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant startAt,
            @Parameter(description = "조회 종료 시각")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant endAt,
            @Parameter(description = "0부터") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "최대 100") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        notificationHistoryService.search(
                                status,
                                templateCode,
                                referenceType,
                                referenceId,
                                startAt,
                                endAt,
                                page,
                                size)));
    }

    @Operation(
            summary = "실패 알림 재발송",
            description =
                    "발송에 실패한 알림 한 건을 다시 보냅니다. 목록의 retryable 이 true 인 것만 됩니다. "
                            + "보낸 본문은 저장하지 않으므로 payload 로 다시 만들어 보냅니다. "
                            + "발권 알림은 링크의 접근 토큰 원문을 되찾을 수 없어 새 토큰을 발급하고 "
                            + "이전 링크는 폐기합니다. "
                            + "대행사가 거절해도 200 이며, 그때는 success 가 false 입니다.")
    @PostMapping("/{notificationId}/retry")
    public ResponseEntity<ApiResponse<NotificationRetryResult>> retryNotification(
            @Parameter(description = "목록의 notificationId", required = true) @PathVariable
                    Long notificationId) {
        return ResponseEntity.ok(ApiResponse.ok(notificationRetryService.retry(notificationId)));
    }
}
