package com.expo.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 토스페이먼츠 결제 승인 API 연동 구현.
 *
 * <p>테스트 키가 아직 없어 실제 토스 서버 응답으로 검증되지 않았다. 실제 키를 받으면 응답 필드 구성이 문서와 일치하는지 다시 확인해야 한다.
 * https://docs.tosspayments.com/reference/using-api/api-keys
 */
@Component
public class TossPaymentClientImpl implements TossPaymentClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final TossPaymentProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TossPaymentClientImpl(TossPaymentProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String getClientKey() {
        requireConfigured(properties.getClientKey(), "TOSS_CLIENT_KEY");
        return properties.getClientKey();
    }

    @Override
    public TossConfirmResult confirmPayment(
            String paymentKey, String orderId, BigDecimal amount, String idempotencyKey) {
        requireConfigured(properties.getSecretKey(), "TOSS_SECRET_KEY");
        requireConfigured(properties.getApiBaseUrl(), "app.toss.api-base-url");
        String credentials =
                Base64.getEncoder()
                        .encodeToString(
                                (properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        try {
            String rawBody =
                    restClient
                            .post()
                            .uri(properties.getApiBaseUrl() + CONFIRM_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                            .header(IDEMPOTENCY_HEADER, idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new TossConfirmApiRequest(paymentKey, orderId, amount))
                            .retrieve()
                            .body(String.class);
            if (rawBody == null || rawBody.isBlank()) {
                throw new TossApiException("EMPTY_RESPONSE", "토스 결제 승인 응답이 비어 있습니다.", null);
            }
            TossConfirmApiResponse response;
            try {
                response = objectMapper.readValue(rawBody, TossConfirmApiResponse.class);
            } catch (JsonProcessingException e) {
                throw new TossApiException(
                        "INVALID_RESPONSE", "토스 결제 승인 응답을 해석할 수 없습니다.", rawBody, e);
            }
            return new TossConfirmResult(
                    response.paymentKey(),
                    response.orderId(),
                    response.status(),
                    response.method(),
                    response.totalAmount(),
                    response.approvedAt(),
                    rawBody);
        } catch (RestClientResponseException e) {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            String code = error != null ? error.code() : "UNKNOWN";
            String message = error != null ? error.message() : e.getMessage();
            throw new TossApiException(code, message, e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new TossApiException(
                    "NETWORK_ERROR", "토스 결제 서버 호출에 실패했습니다: " + e.getMessage(), null, e);
        }
    }

    private void requireConfigured(String value, String settingName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    settingName + " 가 설정되지 않았습니다. 토스페이먼츠 테스트 키를 발급받아 환경변수로 설정하십시오.");
        }
    }

    private record TossConfirmApiRequest(String paymentKey, String orderId, BigDecimal amount) {}

    private record TossConfirmApiResponse(
            String paymentKey,
            String orderId,
            String status,
            String method,
            BigDecimal totalAmount,
            LocalDateTime approvedAt) {}

    private record TossErrorResponse(String code, String message) {}
}
