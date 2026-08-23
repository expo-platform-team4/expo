package com.expo.payment.service;

import com.expo.payment.dto.AdminTicketPaymentHistoryResponse;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 티켓 결제 이력 조회. */
@Service
@RequiredArgsConstructor
public class AdminTicketPaymentHistoryService {
    private final TicketPaymentHistoryRepository ticketPaymentHistoryRepository;

    @Transactional(readOnly = true)
    public List<AdminTicketPaymentHistoryResponse> search(
            String orderNumber, TicketPaymentEventType eventType, int page, int size) {
        return ticketPaymentHistoryRepository.searchAdminHistories(
                orderNumber,
                eventType,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }
}
