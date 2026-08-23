package com.expo.ticket.controller;

import com.expo.common.response.ApiResponse;
import com.expo.ticket.dto.AdminTicketOrderDetailResponse;
import com.expo.ticket.dto.AdminTicketOrderResponse;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.entity.TicketOrdererType;
import com.expo.ticket.service.AdminTicketOrderQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 티켓 주문 검색·상세 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ticket-orders")
public class AdminTicketOrderController {
    private final AdminTicketOrderQueryService adminTicketOrderQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminTicketOrderResponse>>> search(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) TicketOrderStatus status,
            @RequestParam(required = false) TicketOrdererType ordererType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminTicketOrderQueryService.search(
                                orderNumber, status, ordererType, page, size)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AdminTicketOrderDetailResponse>> detail(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(adminTicketOrderQueryService.getDetail(orderId)));
    }
}
