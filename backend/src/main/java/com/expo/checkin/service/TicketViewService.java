package com.expo.checkin.service;

import com.expo.checkin.dto.TicketAccessTokenRow;
import com.expo.checkin.dto.TicketViewResponse;
import com.expo.checkin.dto.TicketViewTicket;
import com.expo.checkin.entity.IssuedTicketStatus;
import com.expo.checkin.entity.TicketAccessTokenStatus;
import com.expo.checkin.repository.TicketViewMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SMS 로 보낸 링크를 열었을 때 티켓과 QR 을 돌려준다.
 *
 * <h2>토큰이 곧 인증이다</h2>
 *
 * 로그인을 요구하지 않는다. 비회원도 문자를 받고, 링크에 실린 256비트 난수는 추측할 수 없기 때문이다. 대신 <b>토큰 원문은 어디에도 남기지 않는다</b> —
 * 로그에도, 예외 메시지에도.
 *
 * <h2>QR 원문은 그때그때 다시 만든다</h2>
 *
 * DB 에는 해시만 있다. 응답에 넣을 원문은 {@code ticket_code} 로 {@link QrTokenGenerator} 를 다시 돌려 만든다. 발권 때와 같은
 * 입력이라 같은 값이 나온다 — 이게 이 설계의 전제다.
 */
@Slf4j
@Service
public class TicketViewService {

    private final TicketViewMapper ticketViewMapper;
    private final QrTokenGenerator qrTokenGenerator;
    private final TokenHasher tokenHasher;

    public TicketViewService(
            TicketViewMapper ticketViewMapper,
            QrTokenGenerator qrTokenGenerator,
            TokenHasher tokenHasher) {
        this.ticketViewMapper = ticketViewMapper;
        this.qrTokenGenerator = qrTokenGenerator;
        this.tokenHasher = tokenHasher;
    }

    /**
     * 접근 토큰으로 그 주문의 입장권을 전부 돌려준다.
     *
     * <p>읽기만 하는 것처럼 보이지만 {@code @Transactional} 이 읽기 전용이 아니다. 접근 기록({@code access_count},
     * {@code last_accessed_at})을 갱신하기 때문이다.
     *
     * @param tokenValue 링크의 {@code ?token=} 값. <b>원문</b>이다
     * @throws BusinessException 토큰이 없거나, 만료됐거나, 폐기된 경우
     */
    @Transactional
    public TicketViewResponse view(String tokenValue) {
        TicketAccessTokenRow token = loadUsableToken(tokenValue);
        List<TicketViewTicket> tickets =
                ticketViewMapper.findTicketsByOrderId(token.ticketOrderId());

        // 링크가 언제 몇 번 열렸는지는 "문자를 못 받았다" 는 문의를 가릴 때 유일한 근거다.
        ticketViewMapper.touchAccessToken(token.id(), Instant.now());

        // 토큰 원문은 남기지 않는다. 이 값이 곧 인증 수단이다.
        log.info(
                "티켓 조회 orderId={} orderNumber={} count={}",
                token.ticketOrderId(),
                token.orderNumber(),
                tickets.size());

        return new TicketViewResponse(
                token.orderNumber(), tickets.size(), tickets.stream().map(this::toTicket).toList());
    }

    /**
     * 토큰을 찾아 쓸 수 있는지 확인한다.
     *
     * <p>실패 이유를 셋으로 나누는 이유는 <b>받는 사람이 할 수 있는 일이 다르기 때문</b>이다. 만료는 재발급을 안내할 수 있지만 폐기는 안내하면 안 된다.
     *
     * <p>다만 <b>없는 토큰과 남의 토큰은 구분하지 않는다.</b> 둘 다 "유효하지 않은 링크" 다 — 구분해 주면 토큰을 넣어 보며 존재 여부를 확인할 수
     * 있게 된다.
     */
    private TicketAccessTokenRow loadUsableToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new BusinessException(ErrorCode.TICKET_ACCESS_TOKEN_NOT_FOUND);
        }

        TicketAccessTokenRow token =
                ticketViewMapper.findAccessTokenByHash(tokenHasher.hash(tokenValue));
        if (token == null) {
            throw new BusinessException(ErrorCode.TICKET_ACCESS_TOKEN_NOT_FOUND);
        }
        if (TicketAccessTokenStatus.REVOKED.name().equals(token.status())) {
            throw new BusinessException(ErrorCode.TICKET_ACCESS_TOKEN_REVOKED);
        }

        // status 와 expires_at 을 둘 다 본다. 만료를 EXPIRED 로 바꿔 주는 배치가 없어서,
        // 기간이 지난 토큰도 status 는 ACTIVE 인 채로 남아 있다.
        boolean expired =
                TicketAccessTokenStatus.EXPIRED.name().equals(token.status())
                        || token.expiresAt().isBefore(Instant.now());
        if (expired) {
            throw new BusinessException(ErrorCode.TICKET_ACCESS_TOKEN_EXPIRED);
        }
        return token;
    }

    /**
     * 입장에 쓸 수 없는 티켓에는 QR 을 주지 않는다.
     *
     * <p>QR 원문은 티켓 코드로만 정해져서, 환불된 티켓도 계산하면 <b>서명이 유효한 값</b>이 나온다. 그대로 내려보내면 환불된 표를 들고 현장에
     * 갔다가 거절당하는 흐름이 된다.
     *
     * <p>최종 방어선은 스캔 시점이다({@code CheckInResult.CANCELED_TICKET}). 여기서 감추는 것은 그 위에 얹는 2차 방어이고,
     * 화면에 "이 표는 못 쓴다" 를 분명히 보여주기 위한 것이기도 하다.
     */
    private static boolean usable(String ticketStatus) {
        return IssuedTicketStatus.ISSUED.name().equals(ticketStatus)
                || IssuedTicketStatus.CHECKED_IN.name().equals(ticketStatus);
    }

    private TicketViewResponse.Ticket toTicket(TicketViewTicket ticket) {
        return new TicketViewResponse.Ticket(
                ticket.issuedTicketId(),
                ticket.ticketCode(),
                usable(ticket.status())
                        ? qrTokenGenerator.generatePayload(ticket.ticketCode())
                        : null,
                ticket.status(),
                ticket.checkedInAt(),
                ticket.expoTitle(),
                ticket.expoStartAt(),
                ticket.expoEndAt());
    }
}
