package com.expo.common.exception;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expo.common.config.SecurityConfig;
import com.expo.jwt.JwtAuthenticationFilter;
import com.expo.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 인가 기본값이 "막는다" 인지, 그리고 열어 둔 경로가 실제로 열려 있는지 못박아 둔다 (이슈 #94).
 *
 * <p>기본값이 {@code permitAll} 이던 시절에는 규칙을 안 적은 컨트롤러가 조용히 공개됐다. 컴파일도 되고 테스트도 통과하고 기동도 되어서 아무도 모른다.
 * 실제로 {@code POST /api/orders/member} 가 인증 없이 호출 가능했고, {@code @AuthenticationPrincipal} 이 {@code null}
 * 이라 500 이 났다.
 *
 * <p>되돌아가면 <b>다음에 추가되는 경로부터</b> 조용히 공개되므로, 그때는 이 테스트가 아니면 아무도 눈치채지 못한다.
 *
 * <p>DB 는 필요 없다. 컨트롤러는 경로 규칙을 태우기 위한 껍데기다 — 200 이 나왔다는 것은 인가를 통과해 컨트롤러까지 닿았다는 뜻이다.
 */
@SpringJUnitWebConfig(SecurityDefaultDenyTest.TestConfig.class)
@TestPropertySource(
        properties = {
            "app.jwt.secret=security-default-deny-test-secret-key-0123456789-abcdef",
            "app.jwt.access-token-expire-minutes=30"
        })
class SecurityDefaultDenyTest {

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

    @RestController
    static class ProbeController {

        /** 어떤 규칙에도 안 걸리는 새 경로. 규칙을 깜빡한 컨트롤러를 흉내 낸다. */
        @GetMapping("/api/brand-new-endpoint")
        String brandNew() {
            return "ok";
        }

        /** 실제로 뚫려 있던 곳. principal 을 그대로 쓰기 때문에 인증 없이 닿으면 안 된다. */
        @PostMapping("/api/orders/member")
        String memberOrder() {
            return "ok";
        }

        // ---------- 공개여야 하는 경로들 ----------

        @GetMapping({
            "/api/expos",
            "/api/expos/1",
            "/api/expos/1/ticket-products/purchasable",
            "/api/recruitment-notices",
            "/api/recruitment-notices/1",
            "/api/virtual-venues",
            "/api/virtual-venues/1/halls",
            "/api/venue-halls/1/zones",
            "/api/files/1",
            "/api/files/1/content",
            "/api/public/tickets",
            "/api/orders/ORD-1/payment-status"
        })
        String publicGet() {
            return "ok";
        }

        @PostMapping({
            "/api/orders/guest",
            "/api/orders/search/guest",
            "/api/orders/guest/refunds",
            "/api/orders/guest/refund-eligibility",
            "/api/payments/initiate",
            "/api/payments/tickets/confirm"
        })
        String publicPost() {
            return "ok";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** 이 테스트가 이 파일의 존재 이유다. */
    @Test
    void pathWithoutAnyRuleRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/brand-new-endpoint")).andExpect(status().isUnauthorized());
    }

    @Test
    void memberOrderCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/orders/member")).andExpect(status().isUnauthorized());
    }

    /** 파일 업로드에는 규칙이 없다 — 기본값이 막아 주는지 확인한다. */
    @Test
    void fileUploadRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/files")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/expos",
                "/api/expos/1",
                "/api/expos/1/ticket-products/purchasable",
                "/api/recruitment-notices",
                "/api/recruitment-notices/1",
                "/api/virtual-venues",
                "/api/virtual-venues/1/halls",
                "/api/venue-halls/1/zones",
                "/api/files/1",
                "/api/files/1/content",
                "/api/public/tickets",
                // 비회원이 결제 후 결과를 확인하는 경로. 게스트는
                // TicketOrderAccessVerifier 가 통과시킨다
                "/api/orders/ORD-1/payment-status"
            })
    void publicGetPathsStayOpen(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/orders/guest",
                "/api/orders/search/guest",
                "/api/orders/guest/refunds",
                "/api/orders/guest/refund-eligibility",
                "/api/payments/initiate",
                "/api/payments/tickets/confirm"
            })
    void guestCheckoutPathsStayOpen(String path) throws Exception {
        mockMvc.perform(post(path)).andExpect(status().isOk());
    }

    /** 읽기만 열었다. 같은 경로에 쓰기가 생기면 기본값이 막아야 한다. */
    @Test
    void writeOnAPublicReadPathIsStillDenied() throws Exception {
        mockMvc.perform(post("/api/expos")).andExpect(status().isUnauthorized());
    }
}
