package com.expo.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 시큐리티 필터가 끊은 요청의 상태 코드와 본문을 못박아 둔다.
 *
 * <p>기본값(둘 다 403, 본문 없음)으로 돌아가면 프론트의 재발급 인터셉터가 401 에서만 돌기 때문에 액세스 토큰이 만료된 뒤 화면이 조용히 실패한다. 눈으로는
 * 30분을 기다려야 보이는 회귀라 여기서 막는다.
 */
class SecurityErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

    @Test
    void unauthenticatedRequestGets401WithApiResponseBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ApiAuthenticationEntryPoint(objectMapper)
                .commence(request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asString())
                .isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED.getMessage());
    }

    @Test
    void insufficientRoleGets403WithApiResponseBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ApiAccessDeniedHandler(objectMapper)
                .handle(request, response, new AccessDeniedException("not admin"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asString()).isEqualTo(ErrorCode.ACCESS_DENIED.getMessage());
    }
}
