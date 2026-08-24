package com.expo.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.ticket.dto.GuestTicketOrderSearchSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** {@link TicketOrderAccessVerifier}의 비회원 인증과 실패 기록 분리를 확인한다. */
class TicketOrderAccessVerifierTest {

    private static final String ORDER_NUMBER = "TICKET-test";
    private static final Long ORDER_ID = 1L;
    private static final String PHONE_NUMBER = "01012345678";
    private static final String PASSWORD = "password";
    private static final String PASSWORD_HASH = "encoded";

    private PasswordEncoder passwordEncoder;
    private GuestTicketOrderSnapshotService snapshotService;
    private GuestTicketOrderAttemptService attemptService;
    private TicketOrderAccessVerifier verifier;

    @BeforeEach
    void setUp() {
        passwordEncoder = mock(PasswordEncoder.class);
        snapshotService = mock(GuestTicketOrderSnapshotService.class);
        attemptService = mock(GuestTicketOrderAttemptService.class);
        verifier = new TicketOrderAccessVerifier(passwordEncoder, snapshotService, attemptService);
    }

    @Test
    void verifyGuestOrderAccessReturnsSnapshotAndResetsFailuresWhenCredentialsMatch() {
        GuestTicketOrderSearchSnapshot snapshot = snapshot();
        when(snapshotService.findByOrderNumber(ORDER_NUMBER)).thenReturn(snapshot);
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        GuestTicketOrderSearchSnapshot result =
                verifier.verifyGuestOrderAccess(ORDER_NUMBER, PHONE_NUMBER, PASSWORD);

        assertThat(result).isSameAs(snapshot);
        verify(attemptService).resetFailures(ORDER_ID);
        verify(attemptService, never()).recordFailure(ORDER_ID);
    }

    @Test
    void verifyGuestOrderAccessRecordsFailureWhenPasswordDoesNotMatch() {
        when(snapshotService.findByOrderNumber(ORDER_NUMBER)).thenReturn(snapshot());
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(
                        () -> verifier.verifyGuestOrderAccess(ORDER_NUMBER, PHONE_NUMBER, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);

        verify(attemptService).recordFailure(ORDER_ID);
    }

    @Test
    void verifyGuestOrderAccessStillEvaluatesPasswordWhenPhoneNumberDoesNotMatch() {
        when(snapshotService.findByOrderNumber(ORDER_NUMBER)).thenReturn(snapshot());
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                verifier.verifyGuestOrderAccess(
                                        ORDER_NUMBER, "01099999999", PASSWORD))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.GUEST_ORDER_LOOKUP_FAILED);

        verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
        verify(attemptService).recordFailure(ORDER_ID);
    }

    private GuestTicketOrderSearchSnapshot snapshot() {
        return new GuestTicketOrderSearchSnapshot(ORDER_ID, PHONE_NUMBER, PASSWORD_HASH, null);
    }
}
