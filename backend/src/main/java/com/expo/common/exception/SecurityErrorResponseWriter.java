package com.expo.common.exception;

import com.expo.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

/**
 * 시큐리티 필터 단계에서 끊긴 요청의 응답 본문을 만든다.
 *
 * <p>필터에서 거부된 요청은 컨트롤러까지 가지 않아 {@link GlobalExceptionHandler} 가 잡을 수 없다. 그래서 여기서 응답을 직접 쓴다. 형식은
 * {@link ApiResponse} 로 맞춘다 — 프론트가 오류를 한 가지 방법으로만 파싱하게 하기 위해서다.
 */
final class SecurityErrorResponseWriter {

    private SecurityErrorResponseWriter() {}

    static void write(ObjectMapper objectMapper, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode.getMessage()));
    }
}
