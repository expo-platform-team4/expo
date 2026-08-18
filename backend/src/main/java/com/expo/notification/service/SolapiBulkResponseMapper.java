package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.SmsMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 대량 발송 응답을 건별 결과로 푼다.
 *
 * <h2>왜 따로 있나</h2>
 *
 * 대행사 응답이 <b>성공과 실패를 다르게</b> 준다. 이 비대칭을 푸는 것이 대량 발송에서 유일하게 까다로운 부분이라,
 * HTTP 호출과 섞어 두면 실제로 쏴 보지 않고는 검증할 수 없다. 떼어 두면 <b>실제로 받은 응답 원문</b>으로
 * 단위 테스트를 할 수 있다.
 *
 * <pre>
 * {
 *   "groupInfo": { "_id": "G4V...", "count": { "registeredSuccess": 1, "registeredFailed": 1 } },
 *   "failedMessageList": [
 *     { "to": "010...", "messageId": "M4V...", "statusCode": "1026",
 *       "statusMessage": "중복 수신번호(...)" }
 *   ]
 * }
 * </pre>
 *
 * <p>성공한 건은 목록에 없다. 개별 {@code messageId} 도 없다. 그래서 <b>실패 목록에 없으면 성공</b>으로 보고,
 * 성공 건의 추적 키로는 <b>그룹 ID</b> 를 쓴다.
 */
@Slf4j
@Component
public class SolapiBulkResponseMapper {

    private final ObjectMapper objectMapper;

    public SolapiBulkResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 요청과 같은 순서·길이의 결과를 돌려준다.
     *
     * @param messages 보낸 메시지들. <b>수신번호가 중복되면 안 된다</b> — 매핑 키가 수신번호다
     * @param responseBody 대행사 응답 원문. 그대로 이력에 저장된다
     * @param requestPayload 이력에 남길 요청 요약
     */
    public List<MessageSendResult> map(
            List<SmsMessage> messages, String responseBody, String requestPayload) {
        JsonNode root = parse(responseBody);
        String groupId = groupId(root);
        Map<String, JsonNode> failedByRecipient = failedByRecipient(root);

        log.info(
                "대량 SMS 발송 접수 groupId={} 요청={}건 실패={}건",
                groupId,
                messages.size(),
                failedByRecipient.size());

        verifyFailureCount(root, failedByRecipient.size(), groupId);

        return messages.stream()
                .map(
                        message ->
                                toResult(
                                        message,
                                        groupId,
                                        failedByRecipient,
                                        responseBody,
                                        requestPayload))
                .toList();
    }

    /**
     * 응답을 한 번만 읽는다. 못 읽으면 {@code null} 이고, 이후 단계는 전부 "실패 목록이 비었다" 로 흐른다.
     *
     * <p>즉 <b>읽지 못하면 전부 성공으로 본다.</b> 발송은 이미 일어났고 원문은 이력에 남으므로, 파싱 실패를
     * 이유로 성공을 실패로 뒤집지 않는다.
     */
    private JsonNode parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (JacksonException e) {
            log.warn("대량 발송 응답을 파싱하지 못했습니다. 원문은 이력에 그대로 남습니다.", e);
            return null;
        }
    }

    /**
     * 대행사가 센 실패 건수와 우리가 짝지은 건수를 대조한다.
     *
     * <h2>왜 필요한가</h2>
     *
     * 짝짓기 키가 <b>수신번호 문자열</b>이다. 우리가 보낸 값과 대행사가 돌려준 값이 한 글자라도 다르면
     * (하이픈이 섞인 번호를 대행사가 정규화해 돌려주는 경우 등) 실패한 건이 <b>조용히 성공으로 기록된다.</b>
     * 문자를 못 받은 사람이 받은 것으로 남는 쪽이라, 로그도 없으면 영원히 모른다.
     *
     * <p>그런데 응답 안에 이미 검산할 값이 들어 있다 — {@code groupInfo.count.registeredFailed}.
     * 우리가 짝지은 개수와 다르면 매핑이 어긋난 것이다. 결과를 바꾸지는 않고 {@code ERROR} 로 남긴다.
     */
    private void verifyFailureCount(JsonNode root, int matched, String groupId) {
        if (root == null) {
            return;
        }
        JsonNode count = root.path("groupInfo").path("count").path("registeredFailed");
        if (count.isMissingNode() || count.isNull()) {
            return;
        }
        int reported = count.asInt();
        if (reported != matched) {
            // 수신번호 표기가 어긋났을 가능성이 높다. 원문은 이력에 그대로 남는다.
            log.error("대량 발송 응답 매핑 불일치 groupId={} 대행사실패={}건 짝지은실패={}건", groupId, reported, matched);
        }
    }

    private MessageSendResult toResult(
            SmsMessage message,
            String groupId,
            Map<String, JsonNode> failedByRecipient,
            String responseBody,
            String requestPayload) {
        JsonNode failed = failedByRecipient.get(message.to());
        if (failed == null) {
            // 성공. 개별 messageId 가 없어 그룹 ID 를 추적 키로 쓴다.
            return MessageSendResult.accepted(groupId, requestPayload, responseBody);
        }
        return MessageSendResult.failed(
                text(failed, "statusCode"), requestPayload, failed.toString());
    }

    /** 실패 목록을 수신번호로 색인한다. */
    private Map<String, JsonNode> failedByRecipient(JsonNode root) {
        Map<String, JsonNode> byRecipient = new LinkedHashMap<>();
        if (root == null) {
            return byRecipient;
        }
        JsonNode list = root.get("failedMessageList");
        if (list != null && list.isArray()) {
            for (JsonNode failed : list) {
                String to = text(failed, "to");
                if (to != null) {
                    byRecipient.put(to, failed);
                }
            }
        }
        return byRecipient;
    }

    private String groupId(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode groupInfo = root.get("groupInfo");
        return groupInfo == null ? null : text(groupInfo, "_id");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
