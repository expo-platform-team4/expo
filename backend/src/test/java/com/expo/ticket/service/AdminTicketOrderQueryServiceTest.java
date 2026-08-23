package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.converter.AdminTicketOrderConverter;
import com.expo.ticket.dto.AdminTicketOrderDetailResponse;
import com.expo.ticket.dto.AdminTicketOrderResponse;
import com.expo.ticket.entity.TicketOrder;
import com.expo.ticket.repository.TicketOrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

/** {@link AdminTicketOrderQueryService}의 관리자 주문 검색·상세 조회를 확인한다. */
class AdminTicketOrderQueryServiceTest {

    private TicketOrderRepository ticketOrderRepository;
    private AdminTicketOrderConverter converter;
    private AdminTicketOrderQueryService service;

    @BeforeEach
    void setUp() {
        ticketOrderRepository = mock(TicketOrderRepository.class);
        converter = mock(AdminTicketOrderConverter.class);
        service = new AdminTicketOrderQueryService(ticketOrderRepository, converter);
    }

    @Test
    void searchConvertsPagedOrders() {
        TicketOrder order = mock(TicketOrder.class);
        AdminTicketOrderResponse response = mock(AdminTicketOrderResponse.class);
        when(ticketOrderRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(converter.toResponse(order)).thenReturn(response);

        assertThat(service.search(null, null, null, 0, 20)).containsExactly(response);
    }

    @Test
    void getDetailRejectsUnknownOrder() {
        when(ticketOrderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_ORDER_NOT_FOUND);
    }

    @Test
    void getDetailConvertsFoundOrder() {
        TicketOrder order = mock(TicketOrder.class);
        AdminTicketOrderDetailResponse response = mock(AdminTicketOrderDetailResponse.class);
        when(ticketOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(converter.toDetailResponse(order)).thenReturn(response);

        assertThat(service.getDetail(1L)).isSameAs(response);
    }
}
