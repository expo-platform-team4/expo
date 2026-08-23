package com.expo.refund.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.repository.TicketRefundRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 관리자 환불 재처리의 실패 상태 검증과 실행 위임을 확인한다. */
class TicketRefundRetryServiceTest {

    private static final Long REFUND_ID = 1L;

    private TicketRefundRepository ticketRefundRepository;
    private TicketRefundExecutionService ticketRefundExecutionService;
    private TicketRefundRetryValidationService validationService;
    private TicketRefundRetryService service;

    @BeforeEach
    void setUp() {
        ticketRefundRepository = mock(TicketRefundRepository.class);
        ticketRefundExecutionService = mock(TicketRefundExecutionService.class);
        validationService = new TicketRefundRetryValidationService(ticketRefundRepository);
        service = new TicketRefundRetryService(validationService, ticketRefundExecutionService);
    }

    @Test
    void retryExecutesOnlyFailedRefund() {
        TicketRefund refund = refund();
        refund.fail("TOSS_ERROR");
        when(ticketRefundRepository.findByIdForUpdate(REFUND_ID)).thenReturn(Optional.of(refund));

        service.retry(REFUND_ID);

        verify(ticketRefundExecutionService).execute(REFUND_ID);
    }

    @Test
    void retryRejectsRequestedRefund() {
        when(ticketRefundRepository.findByIdForUpdate(REFUND_ID)).thenReturn(Optional.of(refund()));

        assertThatThrownBy(() -> service.retry(REFUND_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_NOT_RETRYABLE);
    }

    private TicketRefund refund() {
        return TicketRefund.create(
                1L, 2L, BigDecimal.valueOf(10_000), BigDecimal.valueOf(300), null);
    }
}
