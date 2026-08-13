package com.expo.admin.controller;

import com.expo.admin.dto.AdminAuditLogSearchPage;
import com.expo.admin.service.AdminAuditLogService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 심사·변경·결제·정산 운영 이력 조회 (E-API-018). */
@Tag(name = "Admin Audit Log", description = "관리자 운영 이력 조회")
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @Operation(
            summary = "관리자 운영 이력 조회",
            description =
                    "박람회 심사·변경, 배너 심사, 모집공고 요청·공고, 참여 신청 운영, 티켓·부스 결제 이벤트 이력을 "
                            + "통합 조회합니다. 업무 유형(logType)·대상 ID·상태·기간으로 필터링할 수 있습니다. "
                            + "page·size를 안 주면 첫 페이지(0번, 20건)만 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminAuditLogSearchPage>> searchLogs(
            @Parameter(
                            description =
                                    "업무 유형. EXPO_REVIEW / EXPO_CHANGE / BANNER_REVIEW / "
                                            + "RECRUITMENT_NOTICE_REQUEST / RECRUITMENT_NOTICE / "
                                            + "PARTICIPATION_OPERATION / TICKET_PAYMENT / BOOTH_PAYMENT / "
                                            + "BOOTH_MANAGEMENT")
                    @RequestParam(required = false)
                    String logType,
            @Parameter(description = "대상 ID") @RequestParam(required = false) Long targetId,
            @Parameter(description = "변경 후 상태 필터") @RequestParam(required = false) String toStatus,
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
                        adminAuditLogService.searchLogs(
                                logType, targetId, toStatus, startAt, endAt, page, size)));
    }
}
