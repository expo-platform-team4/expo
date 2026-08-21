package com.expo.checkin.service;

import com.expo.checkin.dto.CheckInResponse;
import com.expo.checkin.entity.CheckInHistory;
import com.expo.checkin.entity.CheckInMethod;
import com.expo.checkin.entity.CheckInResult;
import com.expo.checkin.entity.IssuedTicket;
import com.expo.checkin.repository.CheckInHistoryRepository;
import com.expo.checkin.repository.IssuedTicketRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장에서 QR 또는 티켓 코드를 받아 입장 처리한다.
 *
 * <h2>조회의 반대편이다</h2>
 *
 * {@link TicketViewService} 가 티켓 코드로 QR 원문을 <b>만들어</b> 보여주고, 여기서는 스캔된 원문을 <b>해시해서</b> 티켓을 찾는다.
 * 양쪽이 {@link QrTokenGenerator} 와 {@link TokenHasher} 를 공유하므로 같은 티켓이면 반드시 맞물린다.
 *
 * <pre>
 * 조회   ticket_code ──HMAC──▶ QR 원문 ──▶ 화면
 * 체크인 스캔값 ──SHA-256──▶ qr_token_hash ──▶ 티켓        (UNIQUE 라 조회 키가 된다)
 * </pre>
 *
 * <h2>거절도 결과다</h2>
 *
 * 다섯 갈래 중 넷은 입장 거절이지만 <b>예외가 아니다.</b> 스캐너 화면이 이유를 구분해 보여줘야 하고, 거절도 이력으로 남겨야 하기 때문이다. 위조·미등록 QR 도 마찬가지로 남긴다 — 가리킬 티켓이 없어 {@code issued_ticket_id} 는 비어 있다(이슈 #73).
 */
@Slf4j
@Service
public class CheckInService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final CheckInHistoryRepository checkInHistoryRepository;
    private final ExpoHostVerifier expoHostVerifier;
    private final TokenHasher tokenHasher;
    private final RowLockTimeout rowLockTimeout;

    public CheckInService(
            IssuedTicketRepository issuedTicketRepository,
            CheckInHistoryRepository checkInHistoryRepository,
            ExpoHostVerifier expoHostVerifier,
            TokenHasher tokenHasher,
            RowLockTimeout rowLockTimeout) {
        this.issuedTicketRepository = issuedTicketRepository;
        this.checkInHistoryRepository = checkInHistoryRepository;
        this.expoHostVerifier = expoHostVerifier;
        this.tokenHasher = tokenHasher;
        this.rowLockTimeout = rowLockTimeout;
    }

    /**
     * 스캔한 QR 원문으로 입장 처리한다.
     *
     * <p>스캔값을 그대로 해시해 대조한다. 위조하려면 서명 키가 있어야 하는데 키는 서버에만 있다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckInResponse checkInByQr(
            Long expoId, Long clientUserId, String qrPayload, String requestIp) {
        expoHostVerifier.verifyHost(expoId, clientUserId);

        Optional<IssuedTicket> found =
                isBlank(qrPayload)
                        ? Optional.empty()
                        : rowLockTimeout.runWithTimeout(
                                () ->
                                        issuedTicketRepository.findByQrTokenHash(
                                                tokenHasher.hash(qrPayload)));

        return process(found, expoId, clientUserId, CheckInMethod.QR, requestIp);
    }

    /**
     * 티켓 코드를 손으로 입력해 입장 처리한다. QR 이 안 찍힐 때 쓴다.
     *
     * <p>QR 경로보다 약하다 — 코드는 {@code EXPO-20260810-000004} 처럼 순차적이라 <b>추측할 수 있다.</b> 그래도 두는 이유는
     * 현장에서 스캐너가 고장 나면 줄이 멈추기 때문이다. 대신 누가 수동으로 넣었는지 {@code MANUAL_CODE} 로 이력에 남는다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckInResponse checkInByCode(
            Long expoId, Long clientUserId, String ticketCode, String requestIp) {
        expoHostVerifier.verifyHost(expoId, clientUserId);

        Optional<IssuedTicket> found =
                isBlank(ticketCode)
                        ? Optional.empty()
                        : rowLockTimeout.runWithTimeout(
                                () -> issuedTicketRepository.findByTicketCode(ticketCode.trim()));

        return process(found, expoId, clientUserId, CheckInMethod.MANUAL_CODE, requestIp);
    }

    /**
     * 판정과 기록. 두 경로가 이 흐름을 공유한다.
     *
     * <p>순서가 중요하다. 티켓은 <b>행 잠금과 함께</b> 조회돼 있고, 상태 판정은 그 뒤에 한다. 잠그지 않으면 같은 표를 동시에 찍었을 때 둘 다
     * {@code ISSUED} 를 보고 양쪽 다 통과시킨다.
     */
    private CheckInResponse process(
            Optional<IssuedTicket> found,
            Long expoId,
            Long clientUserId,
            CheckInMethod method,
            String requestIp) {
        Instant now = Instant.now();

        if (found.isEmpty()) {
            // 가리킬 티켓이 없어도 이력에 남긴다 (이슈 #73). 반복 시도가 공격 탐지 신호인데
            // 로그의 IP 는 마스킹되어 있어(docs/logging.md 7절) "같은 출처에서 몇 번" 을 셀 수
            // 없다. 이력에는 request_ip 원문이 들어가므로 SQL 로 집계할 수 있다.
            //
            // 스캔값 자체는 남기지 않는다. 스캐너가 읽은 임의의 문자열이라 무엇이 들어올지
            // 모르고, 추적에 필요한 것은 "언제 어디서 몇 번" 이지 내용이 아니다.
            checkInHistoryRepository.save(
                    CheckInHistory.record(
                            null,
                            expoId,
                            clientUserId,
                            method,
                            CheckInResult.INVALID_TOKEN,
                            now,
                            requestIp,
                            null));
            log.warn(
                    "체크인 실패 (일치하는 티켓 없음) expoId={} method={} ip={}",
                    expoId,
                    method,
                    maskIp(requestIp));
            return reject(CheckInResult.INVALID_TOKEN, null, now);
        }

        IssuedTicket ticket = found.get();

        if (!ticket.getExpoId().equals(expoId)) {
            return rejectAndRecord(
                    ticket, expoId, clientUserId, method, requestIp, now, CheckInResult.WRONG_EXPO);
        }
        if (ticket.isCheckedIn()) {
            return rejectAndRecord(
                    ticket,
                    expoId,
                    clientUserId,
                    method,
                    requestIp,
                    now,
                    CheckInResult.ALREADY_USED);
        }
        if (!ticket.isUsable()) {
            // 환불(CANCELED)·박람회 취소(INVALIDATED). QR 자체는 유효하지만 들여보내면 안 된다.
            return rejectAndRecord(
                    ticket,
                    expoId,
                    clientUserId,
                    method,
                    requestIp,
                    now,
                    CheckInResult.CANCELED_TICKET);
        }

        ticket.checkIn(now);
        saveHistory(ticket, expoId, clientUserId, method, CheckInResult.SUCCESS, requestIp, now);

        log.info(
                "체크인 성공 expoId={} ticketId={} method={} by={}",
                expoId,
                ticket.getId(),
                method,
                clientUserId);

        return new CheckInResponse(
                CheckInResult.SUCCESS.name(),
                true,
                ticket.getId(),
                ticket.getTicketCode(),
                now,
                "입장 처리되었습니다.");
    }

    private CheckInResponse rejectAndRecord(
            IssuedTicket ticket,
            Long expoId,
            Long clientUserId,
            CheckInMethod method,
            String requestIp,
            Instant now,
            CheckInResult result) {
        saveHistory(ticket, expoId, clientUserId, method, result, requestIp, now);
        log.info(
                "체크인 거절 expoId={} ticketId={} result={} method={}",
                expoId,
                ticket.getId(),
                result,
                method);
        return reject(result, ticket, now);
    }

    private void saveHistory(
            IssuedTicket ticket,
            Long expoId,
            Long clientUserId,
            CheckInMethod method,
            CheckInResult result,
            String requestIp,
            Instant now) {
        checkInHistoryRepository.save(
                CheckInHistory.record(
                        ticket.getId(),
                        expoId,
                        clientUserId,
                        method,
                        result,
                        now,
                        requestIp,
                        detailOf(result, ticket)));
    }

    /** 이력의 사람이 읽는 부분. 나중에 "왜 거절됐나" 를 볼 때 이 줄 하나로 끝나야 한다. */
    private String detailOf(CheckInResult result, IssuedTicket ticket) {
        return switch (result) {
            case SUCCESS -> null;
            case WRONG_EXPO -> "티켓의 박람회 = " + ticket.getExpoId();
            case ALREADY_USED -> "먼저 입장한 시각 = " + ticket.getCheckedInAt();
            case CANCELED_TICKET -> "티켓 상태 = " + ticket.getStatus();
            case INVALID_TOKEN -> null;
        };
    }

    private CheckInResponse reject(CheckInResult result, IssuedTicket ticket, Instant now) {
        return new CheckInResponse(
                result.name(),
                false,
                ticket == null ? null : ticket.getId(),
                ticket == null ? null : ticket.getTicketCode(),
                // 이미 입장한 표는 "먼저 입장한 시각" 을 보여줘야 운영자가 상황을 판단할 수 있다.
                result == CheckInResult.ALREADY_USED ? ticket.getCheckedInAt() : null,
                messageOf(result));
    }

    private String messageOf(CheckInResult result) {
        return switch (result) {
            case SUCCESS -> "입장 처리되었습니다.";
            case ALREADY_USED -> "이미 입장한 티켓입니다.";
            case CANCELED_TICKET -> "취소되었거나 무효화된 티켓입니다.";
            case WRONG_EXPO -> "이 박람회의 티켓이 아닙니다.";
            case INVALID_TOKEN -> "확인할 수 없는 티켓입니다.";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 로그에 남길 IP 를 마스킹한다.
     *
     * <p>IP 는 개인 식별이 가능한 값이라 원문을 로그에 남기지 않는다({@code docs/logging.md} 7절이
     * 이메일·전화번호에 요구하는 것과 같은 취급이다). 그렇다고 통째로 빼면 위조 시도를 추적할 근거가
     * 사라지므로, <b>같은 출처에서 반복되는지</b>는 알아볼 수 있는 정도만 남긴다.
     *
     * <pre>
     * 10.20.30.40                → 10.20.30.*
     * 2001:db8::1                → 2001:db8:*
     * </pre>
     *
     * <p>원문이 필요한 감사에는 {@code check_in_histories.request_ip} 를 쓴다. 거기에는 그대로 저장된다 —
     * 접근이 통제되는 테이블과, 수집기로 흘러가면 회수할 수 없는 로그는 다르다.
     */
    private String maskIp(String ip) {
        if (isBlank(ip)) {
            return "unknown";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot + 1) + "*";
        }
        // IPv6. 앞 두 그룹만 남긴다.
        String[] groups = ip.split(":");
        return groups.length >= 2 ? groups[0] + ":" + groups[1] + ":*" : "*";
    }
}
