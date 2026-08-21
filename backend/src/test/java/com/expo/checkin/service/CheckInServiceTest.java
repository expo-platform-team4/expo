package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.checkin.dto.CheckInResponse;
import com.expo.checkin.entity.CheckInHistory;
import com.expo.checkin.entity.CheckInMethod;
import com.expo.checkin.entity.CheckInResult;
import com.expo.checkin.entity.IssuedTicket;
import com.expo.checkin.repository.CheckInHistoryRepository;
import com.expo.checkin.repository.IssuedTicketRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.support.PassthroughRowLockTimeout;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 체크인 판정 규칙을 DB 없이 못박는다.
 *
 * <p>가장 중요한 것은 <b>다섯 갈래가 서로 섞이지 않는가</b>이다. 현장 운영자는 이 결과만 보고 사람을 들여보낼지 정하므로, 거절 사유가 뭉개지면
 * "왜 안 되는지" 를 설명할 수 없다.
 */
class CheckInServiceTest {

    private static final Long EXPO_ID = 1L;
    private static final Long HOST_CLIENT_ID = 7L;
    private static final String TICKET_CODE = "EXPO-20260810-000004";
    private static final String IP = "10.0.0.1";

    private final IssuedTicketRepository issuedTicketRepository =
            Mockito.mock(IssuedTicketRepository.class);
    private final CheckInHistoryRepository checkInHistoryRepository =
            Mockito.mock(CheckInHistoryRepository.class);
    private final ExpoHostVerifier expoHostVerifier = Mockito.mock(ExpoHostVerifier.class);
    private final TokenHasher tokenHasher = new TokenHasher();

    private QrTokenGenerator qrTokenGenerator;
    private CheckInService service;

    @BeforeEach
    void setUp() {
        QrTokenProperties properties = new QrTokenProperties();
        properties.setTokenSecret("test-qr-secret-value-for-unit-test-only");
        qrTokenGenerator = new QrTokenGenerator(properties);

        service =
                new CheckInService(
                        issuedTicketRepository,
                        checkInHistoryRepository,
                        expoHostVerifier,
                        tokenHasher,
                        new PassthroughRowLockTimeout());
    }

    /** 발권이 만드는 것과 같은 모양의 티켓. QR 원문도 실제와 같은 방식으로 만든다. */
    private IssuedTicket ticket(Long expoId, String status, Instant checkedInAt) {
        String qrPayload = qrTokenGenerator.generatePayload(TICKET_CODE);
        IssuedTicket t =
                IssuedTicket.issue(
                        10L, expoId, TICKET_CODE, tokenHasher.hash(qrPayload), Instant.now());
        setField(t, "id", 11L);
        setField(t, "status", com.expo.checkin.entity.IssuedTicketStatus.valueOf(status));
        if (checkedInAt != null) {
            setField(t, "checkedInAt", checkedInAt);
        }
        return t;
    }

    /** 상태·ID 는 DB 가 정하는 값이라 테스트에서만 직접 넣는다. */
    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private String validQr() {
        return qrTokenGenerator.generatePayload(TICKET_CODE);
    }

    private void givenTicket(IssuedTicket t) {
        when(issuedTicketRepository.findByQrTokenHash(t.getQrTokenHash()))
                .thenReturn(Optional.of(t));
        when(issuedTicketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(t));
    }

    private CheckInResult savedResult() {
        ArgumentCaptor<CheckInHistory> captor = ArgumentCaptor.forClass(CheckInHistory.class);
        verify(checkInHistoryRepository).save(captor.capture());
        return captor.getValue().getResult();
    }

    /** 정상 입장. 조회가 만든 QR 이 체크인에서 그대로 맞물려야 한다. */
    @Test
    void admitsValidTicket() {
        IssuedTicket t = ticket(EXPO_ID, "ISSUED", null);
        givenTicket(t);

        CheckInResponse response = service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP);

        assertThat(response.result()).isEqualTo("SUCCESS");
        assertThat(response.admitted()).isTrue();
        assertThat(t.isCheckedIn()).isTrue();
        assertThat(savedResult()).isEqualTo(CheckInResult.SUCCESS);
    }

    /** 같은 표를 두 번 찍으면 두 번째는 막혀야 한다. */
    @Test
    void rejectsAlreadyCheckedInTicket() {
        Instant earlier = Instant.parse("2026-09-01T01:00:00Z");
        givenTicket(ticket(EXPO_ID, "CHECKED_IN", earlier));

        CheckInResponse response = service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP);

        assertThat(response.result()).isEqualTo("ALREADY_USED");
        assertThat(response.admitted()).isFalse();
        // 운영자가 상황을 판단하려면 "언제 먼저 들어갔나" 가 보여야 한다.
        assertThat(response.checkedInAt()).isEqualTo(earlier);
        assertThat(savedResult()).isEqualTo(CheckInResult.ALREADY_USED);
    }

    /** 환불된 표는 QR 서명이 유효해도 들여보내면 안 된다. */
    @Test
    void rejectsCanceledTicket() {
        givenTicket(ticket(EXPO_ID, "CANCELED", null));

        CheckInResponse response = service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP);

        assertThat(response.result()).isEqualTo("CANCELED_TICKET");
        assertThat(savedResult()).isEqualTo(CheckInResult.CANCELED_TICKET);
    }

    /** 박람회 취소로 무효화된 표도 마찬가지다. */
    @Test
    void rejectsInvalidatedTicket() {
        givenTicket(ticket(EXPO_ID, "INVALIDATED", null));

        assertThat(service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP).result())
                .isEqualTo("CANCELED_TICKET");
    }

    /** 다른 박람회 티켓. QR 은 진짜지만 이 현장 것이 아니다. */
    @Test
    void rejectsTicketOfAnotherExpo() {
        givenTicket(ticket(999L, "ISSUED", null));

        CheckInResponse response = service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP);

        assertThat(response.result()).isEqualTo("WRONG_EXPO");
        assertThat(savedResult()).isEqualTo(CheckInResult.WRONG_EXPO);
    }

    /** 다른 박람회 티켓은 <b>입장 처리되면 안 된다.</b> 상태가 그대로여야 한다. */
    @Test
    void doesNotConsumeTicketOfAnotherExpo() {
        IssuedTicket t = ticket(999L, "ISSUED", null);
        givenTicket(t);

        service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP);

        assertThat(t.isCheckedIn()).isFalse();
    }

    /**
     * 위조·미등록 QR <b>도 이력에 남는다</b> (이슈 #73).
     *
     * <p>반복 시도가 공격 탐지 신호인데, 로그의 IP 는 마스킹되어 있어 "같은 출처에서 몇 번" 을 셀 수 없다. 이력에는 원문이 들어간다.
     * 가리킬 티켓이 없으므로 {@code issuedTicketId} 는 비어 있다.
     */
    @Test
    void recordsUnknownQrAttemptWithoutTicket() {
        when(issuedTicketRepository.findByQrTokenHash(any())).thenReturn(Optional.empty());

        CheckInResponse response = service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, "v1.forged", IP);

        assertThat(response.result()).isEqualTo("INVALID_TOKEN");
        assertThat(response.issuedTicketId()).isNull();

        ArgumentCaptor<CheckInHistory> captor = ArgumentCaptor.forClass(CheckInHistory.class);
        verify(checkInHistoryRepository).save(captor.capture());
        CheckInHistory saved = captor.getValue();
        assertThat(saved.getIssuedTicketId()).isNull();
        assertThat(saved.getResult()).isEqualTo(CheckInResult.INVALID_TOKEN);
        assertThat(saved.getExpoId()).isEqualTo(EXPO_ID);
        // 마스킹은 로그의 몫이다. 이력에는 원문이 들어가야 집계할 수 있다.
        assertThat(saved.getRequestIp()).isEqualTo(IP);
    }

    /** 스캔값 자체는 남기지 않는다. 임의 문자열이고, 추적에 필요한 것은 "언제 어디서 몇 번" 이다. */
    @Test
    void doesNotStoreTheScannedPayload() {
        when(issuedTicketRepository.findByQrTokenHash(any())).thenReturn(Optional.empty());

        service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, "v1.forged-payload", IP);

        ArgumentCaptor<CheckInHistory> captor = ArgumentCaptor.forClass(CheckInHistory.class);
        verify(checkInHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDetail()).isNull();
    }

    /** 빈 값으로 DB 를 두드릴 이유가 없다. 다만 시도 자체는 이력에 남는다. */
    @Test
    void rejectsBlankQrWithoutHittingTheDatabase() {
        assertThat(service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, "  ", IP).result())
                .isEqualTo("INVALID_TOKEN");

        verify(issuedTicketRepository, never()).findByQrTokenHash(any());
        verify(checkInHistoryRepository).save(any());
    }

    /**
     * <b>주최자가 아니면 스캔 자체가 막혀야 한다.</b>
     *
     * <p>주최사와 참여 기업이 같은 {@code CLIENT} 역할이라 경로 권한만으로는 못 막는다. 여기가 뚫리면 남의 박람회 입장객을 마음대로 처리할 수 있다.
     */
    @Test
    void rejectsNonHostBeforeTouchingAnyTicket() {
        doThrow(new BusinessException(ErrorCode.NOT_EXPO_HOST))
                .when(expoHostVerifier)
                .verifyHost(EXPO_ID, 999L);

        assertThatThrownBy(() -> service.checkInByQr(EXPO_ID, 999L, validQr(), IP))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_EXPO_HOST);

        verify(issuedTicketRepository, never()).findByQrTokenHash(any());
        verify(checkInHistoryRepository, never()).save(any());
    }

    /** 수동 입력도 같은 판정을 거친다. 다만 이력에 MANUAL_CODE 로 남아야 한다. */
    @Test
    void manualCodePathRecordsItsOwnMethod() {
        givenTicket(ticket(EXPO_ID, "ISSUED", null));

        CheckInResponse response = service.checkInByCode(EXPO_ID, HOST_CLIENT_ID, TICKET_CODE, IP);

        assertThat(response.result()).isEqualTo("SUCCESS");

        ArgumentCaptor<CheckInHistory> captor = ArgumentCaptor.forClass(CheckInHistory.class);
        verify(checkInHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getMethod()).isEqualTo(CheckInMethod.MANUAL_CODE);
    }

    /** 앞뒤 공백이 붙어 들어와도 찾아야 한다. 현장에서 손으로 입력하는 값이다. */
    @Test
    void manualCodeIsTrimmed() {
        givenTicket(ticket(EXPO_ID, "ISSUED", null));

        assertThat(
                        service.checkInByCode(EXPO_ID, HOST_CLIENT_ID, "  " + TICKET_CODE + " ", IP)
                                .result())
                .isEqualTo("SUCCESS");
    }

    /** 이력에는 스캔이 일어난 박람회를 남긴다. 티켓 소속이 아니다 — "이 현장에서 이런 시도가 있었다" 로 읽혀야 한다. */
    @Test
    void historyRecordsTheScanningExpoNotTheTicketExpo() {
        givenTicket(ticket(999L, "ISSUED", null));

        service.checkInByQr(EXPO_ID, HOST_CLIENT_ID, validQr(), IP);

        ArgumentCaptor<CheckInHistory> captor = ArgumentCaptor.forClass(CheckInHistory.class);
        verify(checkInHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getExpoId()).isEqualTo(EXPO_ID);
    }
}
