package com.expo.checkin.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * 발권 티켓 코드를 만든다. 형식은 {@code EXPO-생성일자-일련번호 6자리} 다 (명세 6-7).
 *
 * <pre>
 * EXPO-20260807-000123
 * </pre>
 *
 * <p>일련번호는 <b>호출부가 DB 시퀀스({@code seq_issued_ticket_code})에서 받아 넘긴다.</b> 애플리케이션에서 COUNT 로 세면 동시 발권에
 * 같은 값이 두 번 나오고, {@code ticket_code} 가 UNIQUE 라 그때 INSERT 가 실패한다. 이 클래스를 순수 함수로 두면 단위 테스트가 DB 없이 돈다.
 */
@Component
public class TicketCodeGenerator {

    private static final String PREFIX = "EXPO";
    private static final String SEQUENCE_FORMAT = "%06d";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 코드에 박히는 날짜의 기준 시간대.
     *
     * <p>이 코드는 국내 행사장에서 사람이 눈으로 읽고 대조하는 값이라, 서버 타임존이 아니라 한국 날짜여야 한다. UTC 기준으로 찍으면 밤 9시 이후 발권분의
     * 날짜가 하루 밀린다.
     */
    private static final ZoneId ISSUE_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * @param issuedAt 발권 시각
     * @param sequence {@code seq_issued_ticket_code} 에서 받은 값. 999,999 를 넘으면 자릿수가 자연히 늘어난다
     */
    public String generate(Instant issuedAt, long sequence) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("발권 시각이 없습니다.");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("일련번호는 1 이상이어야 합니다.");
        }
        return PREFIX
                + "-"
                + DATE_FORMAT.format(issuedAt.atZone(ISSUE_ZONE))
                + "-"
                + String.format(SEQUENCE_FORMAT, sequence);
    }
}
