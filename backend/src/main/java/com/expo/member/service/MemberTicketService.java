package com.expo.member.service;

import com.expo.checkin.entity.IssuedTicketStatus;
import com.expo.checkin.service.QrTokenGenerator;
import com.expo.member.dto.MemberTicketGroupResponse;
import com.expo.member.dto.MemberTicketResponse;
import com.expo.member.dto.MemberTicketRow;
import com.expo.member.repository.MemberTicketMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지 "나의 티켓" 조회 (A-API-021, A-API-022).
 *
 * <p>QR 원문은 {@link QrTokenGenerator}로 그때그때 다시 계산한다({@code checkin} 도메인이 SMS 링크 조회
 * ({@code TicketViewService})에서 쓰는 것과 같은 생성기다) — 저장하지 않고 매번 같은 티켓 코드로 같은 값을 만든다.
 * 로그인 사용자 조회라 별도 접근 토큰(access token)은 필요 없다. 뷰가 이미 {@code member_user_id} 로 걸러 주므로
 * JWT 의 principal 이 곧 인증이다.
 */
@Service
@Transactional(readOnly = true)
public class MemberTicketService {

    private final MemberTicketMapper memberTicketMapper;
    private final QrTokenGenerator qrTokenGenerator;

    public MemberTicketService(
            MemberTicketMapper memberTicketMapper, QrTokenGenerator qrTokenGenerator) {
        this.memberTicketMapper = memberTicketMapper;
        this.qrTokenGenerator = qrTokenGenerator;
    }

    /**
     * 로그인한 회원 본인의 발권 티켓을 박람회별로 묶어 돌려준다.
     *
     * <p><b>취소·무효화된 티켓({@code CANCELED}·{@code INVALIDATED})은 아예 제외한다.</b> 그 상태는 "예매
     * 내역"(주문 단위)에서 이미 보여주고 있고, "나의 티켓"은 실제로 입장에 쓸 수 있는 표만 보여주는 화면이라
     * 죽은 티켓까지 카드로 띄우는 건 화면을 어지럽힐 뿐이다. 이 필터링 때문에, 어떤 박람회의 티켓이 전부
     * 취소됐다면 그 박람회 카드 자체가 목록에서 사라진다 — 의도한 동작이다.
     */
    public List<MemberTicketGroupResponse> getMyTickets(Long memberUserId) {
        List<MemberTicketRow> rows = memberTicketMapper.findByMemberUserId(memberUserId);

        Map<Long, MemberTicketGroupBuilder> groups = new LinkedHashMap<>();
        for (MemberTicketRow row : rows) {
            if (!usable(row.status())) {
                continue;
            }
            groups.computeIfAbsent(row.expoId(), id -> new MemberTicketGroupBuilder(row))
                    .tickets
                    .add(toTicketResponse(row));
        }

        return groups.values().stream().map(MemberTicketGroupBuilder::build).toList();
    }

    private MemberTicketResponse toTicketResponse(MemberTicketRow row) {
        return new MemberTicketResponse(
                row.orderId(),
                row.issuedTicketId(),
                row.ticketCode(),
                row.status(),
                row.checkedInAt(),
                usable(row.status()) ? qrTokenGenerator.generatePayload(row.ticketCode()) : null,
                row.orderItemQuantity());
    }

    /**
     * 입장에 쓸 수 없는 티켓에는 QR 을 주지 않는다. {@code TicketViewService.usable} 과 같은 기준이다.
     */
    private static boolean usable(String ticketStatus) {
        return IssuedTicketStatus.ISSUED.name().equals(ticketStatus)
                || IssuedTicketStatus.CHECKED_IN.name().equals(ticketStatus);
    }

    /** 박람회 1개로 묶는 동안 헤더 정보(제목·기간)를 들고 있다가 마지막에 레코드로 굳힌다. */
    private static final class MemberTicketGroupBuilder {
        private final Long expoId;
        private final String expoTitle;
        private final Instant eventStartAt;
        private final Instant eventEndAt;
        private final List<MemberTicketResponse> tickets = new ArrayList<>();

        private MemberTicketGroupBuilder(MemberTicketRow first) {
            this.expoId = first.expoId();
            this.expoTitle = first.expoTitle();
            this.eventStartAt = first.eventStartAt();
            this.eventEndAt = first.eventEndAt();
        }

        private MemberTicketGroupResponse build() {
            return new MemberTicketGroupResponse(
                    expoId, expoTitle, eventStartAt, eventEndAt, tickets);
        }
    }
}
