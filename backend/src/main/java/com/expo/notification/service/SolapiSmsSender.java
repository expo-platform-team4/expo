package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Solapi 로 실제 SMS 를 보낸다. {@code app.sms.provider=solapi} 일 때만 빈으로 만들어진다.
 *
 * <p>공식 SDK 대신 Spring {@link RestClient} 로 직접 호출한다. 인증이 헤더 하나라 구현이 짧고, Boot 4 에서 검증되지 않은 서드파티
 * SDK 를 끌어들이지 않아도 된다.
 *
 * <h2>인증</h2>
 *
 * <pre>
 * Authorization: HMAC-SHA256 apiKey=&lt;키&gt;, date=&lt;ISO8601&gt;, salt=&lt;난수&gt;, signature=&lt;hex&gt;
 * signature = hex( HMAC-SHA256( API Secret, date + salt ) )
 * </pre>
 *
 * <h2>응답을 파싱하지 않는 이유</h2>
 *
 * 응답 본문을 필드로 매핑하지 않고 <b>원문 문자열 그대로</b> {@code message_histories.response_payload}(JSONB) 에
 * 넣는다. 우리에게 필요한 판단은 "대행사가 접수했나"뿐이고 그건 HTTP 상태로 알 수 있다. 스키마에 의존하지 않으니 대행사가 응답 형식을 바꿔도 깨지지 않고,
 * 나중에 필요해지면 저장해 둔 원문에서 꺼내면 된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "solapi")
public class SolapiSmsSender implements SmsSender {

    private static final String SEND_PATH = "/messages/v4/send";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String apiKey;
    private final String apiSecret;
    private final String senderNumber;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SolapiSmsSender(SolapiProperties properties) {
        this.apiKey = required(properties.getApiKey(), "SOLAPI_SMS_API_PUBLIC_KEY");
        this.apiSecret = required(properties.getApiSecret(), "SOLAPI_SMS_API_SECRET_KEY");
        this.senderNumber = required(properties.getSenderNumber(), "SOLAPI_SENDER_NUMBER");
        this.restClient =
                RestClient.builder()
                        .baseUrl(properties.getApiBaseUrl())
                        .requestFactory(requestFactory())
                        .build();
    }

    /**
     * 타임아웃을 <b>반드시</b> 건 요청 팩토리.
     *
     * <p>기본값은 무한 대기다. 이 호출은 커밋 후 스레드에서 일어나므로, 대행사가 응답하지 않으면 그 스레드가 그대로 묶인다. 문자 한 통 때문에 그럴
     * 이유가 없어서 짧게 끊고 실패로 기록한다.
     */
    private static ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /**
     * 설정값을 확인한다. 이 빈이 만들어졌다는 건 실제 발송을 하겠다는 뜻이므로 값이 없으면 기동을 막는다.
     *
     * <p>{@code ${...}} 검사가 있는 이유 — {@code @ConfigurationProperties} 바인딩은 치환하지 못한 플레이스홀더에서
     * 예외를 던지지 않고 원문을 그대로 넘긴다. 환경변수를 빠뜨리면 문자열 {@code "${SOLAPI_...}"} 를 API 키로 들고 기동해 버린다.
     * QR 서명 키에서 실제로 그렇게 통과한 적이 있다.
     */
    private static String required(String value, String envName) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException(envName + " 이(가) 설정되지 않았습니다. backend/.env 를 확인하십시오.");
        }
        return value;
    }

    @Override
    public MessageSendResult send(String to, String text) {
        Map<String, Object> body =
                Map.of("message", Map.of("to", to, "from", senderNumber, "text", text));
        // 수신번호가 들어 있어 로그에 남기지 않는다. 이력 테이블에만 저장한다.
        String requestPayload = toJsonForRecord(to, text);

        try {
            ResponseEntity<String> response =
                    restClient
                            .post()
                            .uri(SEND_PATH)
                            .header("Authorization", authorizationHeader())
                            .body(body)
                            .retrieve()
                            .toEntity(String.class);

            String responseBody = response.getBody();
            String statusCode = readField(responseBody, "statusCode");
            String messageId = readField(responseBody, "messageId");

            // 200 안에 실패가 담겨 오는 경우가 있다. 접수 성공은 2000 번대다.
            if (statusCode != null && !statusCode.startsWith("2")) {
                log.warn("SMS 발송 거부 statusCode={}", statusCode);
                return MessageSendResult.failed(statusCode, requestPayload, responseBody);
            }

            log.info("SMS 발송 접수 statusCode={} messageId={}", statusCode, messageId);
            return MessageSendResult.accepted(messageId, requestPayload, responseBody);

        } catch (RestClientResponseException e) {
            // 대행사가 4xx/5xx 를 돌려준 경우. 응답 본문에 실패 사유가 들어 있다.
            log.warn("SMS 발송 거부 status={}", e.getStatusCode().value());
            return MessageSendResult.failed(
                    "HTTP_" + e.getStatusCode().value(),
                    requestPayload,
                    e.getResponseBodyAsString());

        } catch (RestClientException e) {
            // 연결 실패·타임아웃 등 응답 자체가 없는 경우.
            log.warn("SMS 발송 실패 (응답 없음)", e);
            return MessageSendResult.failed("TRANSPORT_ERROR", requestPayload, null);
        }
    }

    /**
     * 응답에서 문자열 필드 하나를 꺼낸다. 없거나 파싱에 실패하면 {@code null}.
     *
     * <p>응답 전체를 DTO 로 매핑하지 않는 이유는 앞의 클래스 주석과 같다 — 대행사가 필드를 추가·변경해도 깨지지 않아야 한다. 여기서 꺼내는
     * 두 값을 못 읽어도 발송 자체는 이미 끝났고 원문은 이력에 남으므로, 실패로 뒤집지 않고 {@code null} 로 넘긴다.
     */
    private String readField(String responseBody, String fieldName) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode field = objectMapper.readTree(responseBody).get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        } catch (JsonProcessingException e) {
            log.warn("Solapi 응답을 파싱하지 못했습니다. 원문은 이력에 그대로 남습니다.");
            return null;
        }
    }

    /** 요청 이력용 JSON. 본문은 길고 링크 토큰이 들어 있어 길이만 남긴다. */
    private String toJsonForRecord(String to, String text) {
        return "{\"to\":\""
                + to
                + "\",\"from\":\""
                + senderNumber
                + "\",\"textLength\":"
                + text.length()
                + "}";
    }

    private String authorizationHeader() {
        String date =
                DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        String salt = randomSalt();
        String signature = sign(date + salt);
        return "HMAC-SHA256 apiKey="
                + apiKey
                + ", date="
                + date
                + ", salt="
                + salt
                + ", signature="
                + signature;
    }

    private String randomSalt() {
        byte[] buffer = new byte[SALT_BYTES];
        secureRandom.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }

    private String sign(String message) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(
                    new SecretKeySpec(
                            apiSecret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Solapi 요청 서명에 실패했습니다.", e);
        }
    }
}
