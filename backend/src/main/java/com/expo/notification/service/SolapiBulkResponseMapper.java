package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.SmsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 요청과 같은 순서·길이의 결과를 돌려준다.
     *
     * @param messages 보낸 메시지들. <b>수신번호가 중복되면 안 된다</b> — 매핑 키가 수신번호다
     * @param responseBody 대행사 응답 원문. 그대로 이력에 저장된다
     * @param requestPayload 이력에 남길 요청 요약
     */
    public List<MessageSendResult> map(
            List<SmsMessage> messages, String responseBody, String requestPayload) {
        String groupId = groupId(responseBody);
        Map<String, JsonNode> failedByRecipient = failedByRecipient(responseBody);

        log.info(
                "대량 SMS 발송 접수 groupId={} 요청={}건 실패={}건",
                groupId,
                messages.size(),
                failedByRecipient.size());

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

    /**
     * 실패 목록을 수신번호로 색인한다.
     *
     * <p>파싱에 실패하면 빈 맵을 돌려준다 — 즉 <b>전부 성공으로 본다.</b> 발송은 이미 일어났고 응답 원문은
     * 이력에 남으므로, 읽지 못했다는 이유로 성공을 실패로 뒤집지 않는다.
     */
    private Map<String, JsonNode> failedByRecipient(String responseBody) {
        Map<String, JsonNode> byRecipient = new LinkedHashMap<>();
        if (responseBody == null || responseBody.isBlank()) {
            return byRecipient;
        }
        try {
            JsonNode list = objectMapper.readTree(responseBody).get("failedMessageList");
            if (list != null && list.isArray()) {
                for (JsonNode failed : list) {
                    String to = text(failed, "to");
                    if (to != null) {
                        byRecipient.put(to, failed);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("대량 발송 응답을 파싱하지 못했습니다. 원문은 이력에 그대로 남습니다.");
        }
        return byRecipient;
    }

    private String groupId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode groupInfo = objectMapper.readTree(responseBody).get("groupInfo");
            return groupInfo == null ? null : text(groupInfo, "_id");
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
