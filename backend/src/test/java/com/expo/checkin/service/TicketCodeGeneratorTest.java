package com.expo.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 티켓 코드가 명세 형식({@code EXPO-생성일자-일련번호 6자리})과 컬럼 폭을 지키는지 검증한다. */
class TicketCodeGeneratorTest {

    /** issued_tickets.ticket_code 는 VARCHAR(50) 이다. */
    private static final int TICKET_CODE_COLUMN_LENGTH = 50;

    private final TicketCodeGenerator generator = new TicketCodeGenerator();

    @Test
    void formatsAsSpecified() {
        String code = generator.generate(Instant.parse("2026-08-07T01:00:00Z"), 123L);

        assertThat(code).isEqualTo("EXPO-20260807-000123");
    }

    @Test
    void padsSequenceToSixDigits() {
        String code = generator.generate(Instant.parse("2026-08-07T01:00:00Z"), 1L);

        assertThat(code).endsWith("-000001");
    }

    /** 6자리를 넘으면 자릿수가 늘어난다. 잘라내면 UNIQUE 충돌이 난다. */
    @Test
    void keepsAllDigitsWhenSequenceOverflowsSixDigits() {
        String code = generator.generate(Instant.parse("2026-08-07T01:00:00Z"), 1_234_567L);

        assertThat(code).isEqualTo("EXPO-20260807-1234567");
    }

    /**
     * 날짜는 한국 시간 기준이다.
     *
     * <p>현장에서 사람이 읽고 대조하는 값이라 서버 타임존을 따라가면 안 된다. 아래 UTC 시각은 KST 로는 다음 날 00:30 이므로 코드에는 08-08 이
     * 찍혀야 한다. UTC 로 찍으면 하루가 밀린다.
     */
    @Test
    void usesKoreanDateNotUtcDate() {
        String code = generator.generate(Instant.parse("2026-08-07T15:30:00Z"), 1L);

        assertThat(code).startsWith("EXPO-20260808-");
    }

    @Test
    void fitsInTheColumn() {
        String code = generator.generate(Instant.parse("2026-08-07T01:00:00Z"), Long.MAX_VALUE);

        assertThat(code).hasSizeLessThanOrEqualTo(TICKET_CODE_COLUMN_LENGTH);
    }

    @Test
    void rejectsNonPositiveSequence() {
        Instant issuedAt = Instant.parse("2026-08-07T01:00:00Z");

        assertThatThrownBy(() -> generator.generate(issuedAt, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingIssuedAt() {
        assertThatThrownBy(() -> generator.generate(null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
