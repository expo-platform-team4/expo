package com.expo.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

/**
 * MDC 정리가 실제로 되는지 검증한다.
 *
 * <p>MDC 는 ThreadLocal 이고 톰캣은 스레드를 풀에서 재사용한다. 정리를 빠뜨리면 다음 요청의 로그에 이전 사용자의 {@code memberId} 가 그대로
 * 붙는다. 개인정보가 엉뚱한 요청에 섞이는 사고라, 눈으로 확인할 수 없는 이 동작을 테스트로 못박아 둔다.
 */
class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();
    private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    private final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void traceIdIsPresentDuringChain() throws Exception {
        String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get(MdcLoggingFilter.TRACE_ID);

        filter.doFilterInternal(request, response, chain);

        assertThat(captured[0]).isNotNull().hasSize(8);
    }

    @Test
    void mdcIsClearedAfterRequest() throws Exception {
        filter.doFilterInternal(request, response, (req, res) -> {});

        assertThat(MDC.get(MdcLoggingFilter.TRACE_ID)).isNull();
    }

    @Test
    void mdcIsClearedEvenWhenChainThrows() {
        FilterChain throwing =
                (req, res) -> {
                    // 인증 필터가 넣는 값을 흉내낸다. 예외가 나도 같이 지워져야 한다.
                    MDC.put(MdcLoggingFilter.MEMBER_ID, "42");
                    throw new IllegalStateException("boom");
                };

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, throwing))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(MdcLoggingFilter.TRACE_ID)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.MEMBER_ID)).isNull();
    }

    @Test
    void traceIdDiffersPerRequest() throws Exception {
        String[] first = new String[1];
        String[] second = new String[1];

        filter.doFilterInternal(
                request, response, (req, res) -> first[0] = MDC.get(MdcLoggingFilter.TRACE_ID));
        filter.doFilterInternal(
                request, response, (req, res) -> second[0] = MDC.get(MdcLoggingFilter.TRACE_ID));

        assertThat(first[0]).isNotEqualTo(second[0]);
    }
}
