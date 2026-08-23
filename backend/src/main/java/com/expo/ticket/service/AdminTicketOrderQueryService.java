package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.converter.AdminTicketOrderConverter;
import com.expo.ticket.dto.AdminTicketOrderDetailResponse;
import com.expo.ticket.dto.AdminTicketOrderResponse;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.entity.TicketOrderStatus;
import com.expo.ticket.entity.TicketOrdererType;
import com.expo.ticket.repository.TicketOrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 티켓 주문 목록·상세 조회. */
@Service
@RequiredArgsConstructor
public class AdminTicketOrderQueryService {

    private final TicketOrderRepository ticketOrderRepository;
    private final AdminTicketOrderConverter adminTicketOrderConverter;

    @Transactional(readOnly = true)
    public List<AdminTicketOrderResponse> search(
            String orderNumber,
            TicketOrderStatus status,
            TicketOrdererType ordererType,
            int page,
            int size) {
        Specification<TicketOrder> specification =
                Specification.allOf(
                        containsOrderNumber(orderNumber),
                        hasStatus(status),
                        hasOrdererType(ordererType));
        return ticketOrderRepository
                .findAll(
                        specification,
                        PageRequest.of(
                                Math.max(page, 0),
                                Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(adminTicketOrderConverter::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public AdminTicketOrderDetailResponse getDetail(Long orderId) {
        TicketOrder order =
                ticketOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_ORDER_NOT_FOUND));
        return adminTicketOrderConverter.toDetailResponse(order);
    }

    private Specification<TicketOrder> containsOrderNumber(String orderNumber) {
        return (root, query, builder) ->
                orderNumber == null || orderNumber.isBlank()
                        ? builder.conjunction()
                        : builder.like(root.get("orderNumber"), "%" + orderNumber + "%");
    }

    private Specification<TicketOrder> hasStatus(TicketOrderStatus status) {
        return (root, query, builder) ->
                status == null ? builder.conjunction() : builder.equal(root.get("status"), status);
    }

    private Specification<TicketOrder> hasOrdererType(TicketOrdererType ordererType) {
        return (root, query, builder) ->
                ordererType == null
                        ? builder.conjunction()
                        : builder.equal(root.get("ordererType"), ordererType);
    }
}
