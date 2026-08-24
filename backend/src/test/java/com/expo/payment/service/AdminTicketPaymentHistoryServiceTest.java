package com.expo.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.payment.dto.AdminTicketPaymentHistoryResponse;
import com.expo.payment.entity.TicketPaymentEventType;
import com.expo.payment.repository.TicketPaymentHistoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link AdminTicketPaymentHistoryService}의 관리자 결제 이력 검색을 확인한다. */
class AdminTicketPaymentHistoryServiceTest {

    private TicketPaymentHistoryRepository ticketPaymentHistoryRepository;
    private AdminTicketPaymentHistoryService service;

    @BeforeEach
    void setUp() {
        ticketPaymentHistoryRepository = mock(TicketPaymentHistoryRepository.class);
        service = new AdminTicketPaymentHistoryService(ticketPaymentHistoryRepository);
    }

    @Test
    void searchReturnsRepositoryHistories() {
        AdminTicketPaymentHistoryResponse response = mock(AdminTicketPaymentHistoryResponse.class);
        when(ticketPaymentHistoryRepository.searchAdminHistories(
                        org.mockito.ArgumentMatchers.eq("TICKET-test"),
                        org.mockito.ArgumentMatchers.eq(TicketPaymentEventType.APPROVE),
                        any()))
                .thenReturn(List.of(response));

        assertThat(service.search("TICKET-test", TicketPaymentEventType.APPROVE, 0, 20))
                .containsExactly(response);
    }
}
