package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * "나의 티켓" 화면 카드 하나 — 박람회 1개 기준으로 그 박람회에서 산 티켓을 전부 묶는다.
 *
 * <p>회원 1명이 같은 박람회를 여러 번 주문했을 수 있어(추가 예매), 주문이 아니라 <b>박람회</b> 단위로 묶는다. 화면은 카드 하나에 박람회
 * 제목·사진을 보여주고 오른쪽에서 {@code tickets} 를 좌우로 넘겨 보여준다.
 */
@Schema(description = "박람회별 내 티켓 묶음")
public record MemberTicketGroupResponse(
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String expoTitle,
        @Schema(description = "행사 시작") Instant eventStartAt,
        @Schema(description = "행사 종료") Instant eventEndAt,
        @Schema(description = "이 박람회에서 발권된 내 티켓 목록") List<MemberTicketResponse> tickets) {}
