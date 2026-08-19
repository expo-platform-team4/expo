package com.expo.settlement.dto;

import java.time.Instant;

/**
 * 정산을 만들어야 할 박람회 하나.
 *
 * @param hostClientId 정산금을 받을 주최사. {@code client_profiles.user_id} 다 — {@code users.id} 가
 *     아니라는 점에 주의한다. FK 가 그렇게 걸려 있다
 * @param eventEndAt 행사 종료. 정산 기한을 여기서 센다
 */
public record SettlementTarget(
        Long expoId, Long hostClientId, String expoTitle, Instant eventEndAt) {}
