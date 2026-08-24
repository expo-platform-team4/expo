package com.expo.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 로그인은 했지만 역할이 모자란 요청에 403 을 돌려준다. 예: MEMBER 토큰으로 {@code /api/admin/**} 호출.
 *
 * <p>기본 핸들러도 403 을 주지만 본문이 비어 있어 프론트가 이유를 보여줄 수 없다. 401 과 달리 여기서는 재발급해도 달라지는 게 없으므로 상태 코드는 403 을
 * 유지한다.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        SecurityErrorResponseWriter.write(objectMapper, response, ErrorCode.ACCESS_DENIED);
    }
}
