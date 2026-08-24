package com.expo.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증되지 않은 요청에 401 을 돌려준다.
 *
 * <p>등록하지 않으면 Spring Security 가 {@code Http403ForbiddenEntryPoint} 를 쓴다 (formLogin·httpBasic 을 모두
 * 껐기 때문이다). 그러면 토큰이 없거나 만료된 요청도 403 으로 나가고, 프론트의 재발급 인터셉터는 401 에서만 도는 탓에 액세스 토큰 수명이 지난 뒤 요청이
 * 조용히 실패한다.
 *
 * <p>권한이 모자란 경우는 여기가 아니라 {@link ApiAccessDeniedHandler} 가 403 으로 처리한다.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        SecurityErrorResponseWriter.write(
                objectMapper, response, ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
