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
 * 다섯 갈래 중 넷은 입장 거절이지만 <b>예외가 아니다.</b> 스캐너 화면이 이유를 구분해 보여줘야 하고, 거절도 이력으로 남겨야 하기 때문이다.
 */
@Slf4j
@Service
public class CheckInService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final CheckInHistoryRepository checkInHistoryRepository;
    private final ExpoHostVerifier expoHostVerifier;
    private final TokenHasher tokenHasher;

    public CheckInService(
            IssuedTicketRepository issuedTicketRepository,
            CheckInHistoryRepository checkInHistoryRepository,
            ExpoHostVerifier expoHostVerifier,
            TokenHasher tokenHasher) {
        this.issuedTicketRepository = issuedTicketRepository;
        this.checkInHistoryRepository = checkInHistoryRepository;
        this.expoHostVerifier = expoHostVerifier;
        this.tokenHasher = tokenHasher;
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
                        : issuedTicketRepository.findByQrTokenHash(tokenHasher.hash(qrPayload));

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
                        : issuedTicketRepository.findByTicketCode(ticketCode.trim());

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
            // 가리킬 티켓이 없어 check_in_histories 에 남길 수 없다 (issued_ticket_id 가 NOT NULL).
            // 위조 시도일 수 있으므로 로그로는 반드시 남긴다.
            log.warn("체크인 실패 (일치하는 티켓 없음) expoId={} method={} ip={}", expoId, method, requestIp);
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
}
