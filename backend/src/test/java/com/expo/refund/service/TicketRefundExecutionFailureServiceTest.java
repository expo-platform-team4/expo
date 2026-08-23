package com.expo.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.refund.entity.TicketRefund;
import com.expo.refund.entity.TicketRefundStatus;
import com.expo.refund.repository.TicketRefundRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link TicketRefundExecutionFailureService}의 잠금 조회 기반 실패 기록을 확인한다. */
class TicketRefundExecutionFailureServiceTest {

    private static final Long REFUND_ID = 1L;

    private TicketRefundRepository ticketRefundRepository;
    private TicketRefundExecutionFailureService service;

    @BeforeEach
    void setUp() {
        ticketRefundRepository = mock(TicketRefundRepository.class);
        service = new TicketRefundExecutionFailureService(ticketRefundRepository);
    }

    @Test
    void recordFailureLocksRefundAndMarksItFailed() {
        TicketRefund refund = refund();
        when(ticketRefundRepository.findByIdForUpdate(REFUND_ID)).thenReturn(Optional.of(refund));

        service.recordFailure(REFUND_ID, "TOSS_ERROR");

        assertThat(refund.getStatus()).isEqualTo(TicketRefundStatus.FAILED);
        assertThat(refund.getLastFailureCode()).isEqualTo("TOSS_ERROR");
        verify(ticketRefundRepository).findByIdForUpdate(REFUND_ID);
    }

    @Test
    void recordFailureRejectsWhenRefundDoesNotExist() {
        when(ticketRefundRepository.findByIdForUpdate(REFUND_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordFailure(REFUND_ID, "TOSS_ERROR"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_NOT_FOUND);
    }

    private TicketRefund refund() {
        return TicketRefund.create(
                1L, 2L, BigDecimal.valueOf(10_000), BigDecimal.valueOf(300), null);
    }
}
