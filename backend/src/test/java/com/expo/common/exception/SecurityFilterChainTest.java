package com.expo.common.exception;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expo.auth.Role;
import com.expo.common.config.SecurityConfig;
import com.expo.jwt.JwtAuthenticationFilter;
import com.expo.jwt.JwtProperties;
import com.expo.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 실제 시큐리티 필터 체인을 태워 인증 실패(401)와 권한 부족(403)을 갈라 놓는다.
 *
 * <p>{@link SecurityErrorResponseTest} 가 핸들러 하나하나를 보는 것과 달리, 여기서는 {@link SecurityConfig} 가 그 핸들러를
 * 실제로 물고 있는지까지 확인한다. 등록을 빠뜨리면 기본값인 {@code Http403ForbiddenEntryPoint} 가 살아나 토큰 만료도 403 으로 나가고,
 * 프론트의 재발급 인터셉터(401 에서만 동작)가 침묵한다.
 *
 * <p>DB 는 필요 없다. 컨트롤러도 경로 규칙을 태우기 위한 껍데기만 둔다.
 */
@SpringJUnitWebConfig(SecurityFilterChainTest.TestConfig.class)
@TestPropertySource(
        properties = {
            "app.jwt.secret=security-filter-chain-test-secret-key-0123456789-abcdefghij",
            "app.jwt.access-token-expire-minutes=30"
        })
class SecurityFilterChainTest {

    private static final String TEST_SECRET =
            "security-filter-chain-test-secret-key-0123456789-abcdefghij";

    /** 부트 자동설정 없이 시큐리티 체인만 띄운다. 슬라이스 테스트로 열면 JPA 감사 설정까지 딸려 온다. */
    @Configuration
    @EnableWebMvc
    @Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class
    })
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    /** URL 규칙만 태우기 위한 껍데기. 여기까지 왔다는 것은 인가를 통과했다는 뜻이다. */
    @RestController
    static class ProbeController {

        @GetMapping("/api/users/me")
        String me() {
            return "me";
        }

        @GetMapping("/api/admin/users")
        String adminUsers() {
            return "admin";
        }
    }

    @Autowired private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void noTokenGets401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));
    }

    @Test
    void forgedTokenGets401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));
    }

    @Test
    void expiredTokenGets401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(expiredMemberToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));
    }

    @Test
    void memberTokenOnAdminPathGets403() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, Role.MEMBER);

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @Test
    void memberTokenOnOwnPathPassesThrough() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, Role.MEMBER);

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    /** 만료 시간이 음수인 발급기로 이미 지난 토큰을 만든다. 30분을 기다리지 않고 만료를 재현하는 방법이다. */
    private String expiredMemberToken() {
        JwtProperties expired = new JwtProperties();
        expired.setSecret(TEST_SECRET);
        expired.setAccessTokenExpireMinutes(-1);
        return new JwtTokenProvider(expired).createAccessToken(1L, Role.MEMBER);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
