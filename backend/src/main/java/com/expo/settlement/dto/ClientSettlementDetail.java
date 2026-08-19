package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 정산 상세 (D-API-012). 요약에 <b>구성 항목</b>을 붙인 것이다.
 *
 * <p>WBS 가 "티켓/부스 구분 리포트" 라고 부르는 것이 {@code items} 다. 종류(TICKET_SALE,
 * BOOTH_SALE …)로 갈려 있고 {@code includedInRemittance} 가 정산금 포함 여부를 알려 준다.
 *
 * <p>파일로 내려받는 리포트는 <b>이 API 가 만들지 않는다.</b> 파일 저장 도메인이 아직 없어서
 * <a href="https://github.com/expo-platform-team4/expo/issues/93">이슈 #93</a> 으로 빼 뒀다.
 * 요약의 {@code latestReportFileId} 가 그 자리를 미리 잡아 두고 있다.
 */
@Schema(description = "정산 상세")
public record ClientSettlementDetail(
        @Schema(description = "정산 요약") ClientSettlementResponse settlement,
        @Schema(description = "금액 구성 항목") List<SettlementItemResponse> items) {}
