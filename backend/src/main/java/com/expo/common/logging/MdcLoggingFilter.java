package com.expo.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 {@code traceId} 를 MDC 에 넣어 로그를 요청 단위로 묶는다.
 *
 * <p>여러 요청의 로그가 섞이면 읽을 수 없다. 이 필터가 넣은 {@code traceId} 로 한 요청의 로그만 골라낼 수 있다. 로컬은 콘솔 패턴의 {@code
 * [%X{traceId}]} 자리에, 운영은 ECS JSON 의 필드로 나간다.
 *
 * <p>{@link Ordered#HIGHEST_PRECEDENCE} 로 가장 먼저 실행된다. 인증 실패도 추적해야 하므로 {@code
 * JwtAuthenticationFilter} 보다 앞에 있어야 한다. {@code memberId} 는 인증에 성공한 시점에 그 필터가 추가로 넣는다.
 *
 * <p>MDC 키 이름은 {@code docs/logging.md} 에 고정돼 있다. 임의로 바꾸면 수집기에서 필드가 갈라진다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    /** 로그를 요청 단위로 묶는 키. */
    public static final String TRACE_ID = "traceId";

    /** 인증된 회원 PK. {@code JwtAuthenticationFilter} 가 넣는다. */
    public static final String MEMBER_ID = "memberId";

    /** UUID 전체는 로그 한 줄을 잡아먹는다. 한 요청을 구분하는 데는 앞 8자리로 충분하다. */
    private static final int TRACE_ID_LENGTH = 8;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0, TRACE_ID_LENGTH));
            filterChain.doFilter(request, response);
        } finally {
            // MDC 는 ThreadLocal 이고 톰캣은 스레드를 풀에서 재사용한다.
            // 지우지 않으면 다음 요청의 로그에 이전 사용자의 memberId 가 그대로 붙는다.
            // 예외가 나도 반드시 지워야 하므로 finally 에 둔다.
            MDC.clear();
        }
    }
}
