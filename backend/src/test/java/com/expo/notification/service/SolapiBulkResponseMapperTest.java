package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.SmsMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 대량 발송 응답 매핑을 못박는다.
 *
 * <p>여기 쓰인 JSON 은 지어낸 것이 아니라 <b>Solapi 에 실제로 2건을 보내고 받은 응답</b>이다. 문서에서 스펙을 얻지 못해
 * 직접 쏴서 확인했고, 그 원문을 테스트에 고정해 둔다 — 대행사가 형식을 바꾸면 여기서 드러나야 한다.
 *
 * <p>핵심은 <b>성공과 실패의 응답 방식이 다르다</b>는 것이다. 실패만 개별로 오고 성공은 개수만 온다.
 */
class SolapiBulkResponseMapperTest {

    /** 실제 응답. 두 건을 같은 번호로 보내 하나가 중복으로 걸린 상황이다. */
    private static final String REAL_RESPONSE =
            """
            {
              "groupInfo": {
                "_id": "G4V20260813185432L0UOCCFAV9BAHIX",
                "count": { "total": 2, "registeredSuccess": 1, "registeredFailed": 1 }
              },
              "failedMessageList": [
                {
                  "to": "01045770340",
                  "from": "01045770340",
                  "type": "SMS",
                  "statusMessage": "중복 수신번호(동일한 수신번호로 전송했을 때, 중복 전송을 방지하기 위해 1개의 메시지를 제외 한 다른 메시지를 실패 처리 합니다.)",
                  "country": "82",
                  "messageId": "M4V20260813185445L6FHQ7KOM6LXYTW",
                  "statusCode": "1026",
                  "accountId": "26080705851281"
                }
              ]
            }
            """;

    private static final String GROUP_ID = "G4V20260813185432L0UOCCFAV9BAHIX";

    private final SolapiBulkResponseMapper mapper = new SolapiBulkResponseMapper();

    private SmsMessage message(String to) {
        return new SmsMessage(to, "본문");
    }

    /** 실패 목록에 있는 번호는 실패다. 대행사가 준 statusCode 를 그대로 남긴다. */
    @Test
    void marksRecipientInFailedListAsFailed() {
        List<MessageSendResult> results =
                mapper.map(List.of(message("01045770340")), REAL_RESPONSE, "{}");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(0).errorCode()).isEqualTo("1026");
    }

    /**
     * <b>실패 목록에 없으면 성공이다.</b>
     *
     * <p>응답이 성공 건을 따로 알려 주지 않기 때문에 이렇게 판정할 수밖에 없다.
     */
    @Test
    void treatsRecipientAbsentFromFailedListAsSuccess() {
        List<MessageSendResult> results =
                mapper.map(List.of(message("01099998888")), REAL_RESPONSE, "{}");

        assertThat(results.get(0).success()).isTrue();
    }

    /** 성공 건은 개별 messageId 가 없다. 추적 키로 그룹 ID 를 쓴다. */
    @Test
    void usesGroupIdAsTrackingKeyForSuccess() {
        List<MessageSendResult> results =
                mapper.map(List.of(message("01099998888")), REAL_RESPONSE, "{}");

        assertThat(results.get(0).providerMessageId()).isEqualTo(GROUP_ID);
    }

    /** 결과는 <b>요청과 같은 순서·길이</b>여야 한다. 호출부가 i 번째끼리 짝지어 쓴다. */
    @Test
    void keepsRequestOrderAndSize() {
        List<MessageSendResult> results =
                mapper.map(
                        List.of(
                                message("01011112222"),
                                message("01045770340"), // 실패한 번호
                                message("01033334444")),
                        REAL_RESPONSE,
                        "{}");

        assertThat(results).hasSize(3);
        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(1).success()).isFalse();
        assertThat(results.get(2).success()).isTrue();
    }

    /** 실패 목록이 아예 없는 응답. 전부 성공이다. */
    @Test
    void treatsAllAsSuccessWhenNoFailedList() {
        String allSucceeded =
                """
                {"groupInfo":{"_id":"G4V-ALL-OK","count":{"total":2,"registeredSuccess":2}}}
                """;

        List<MessageSendResult> results =
                mapper.map(
                        List.of(message("01011112222"), message("01033334444")),
                        allSucceeded,
                        "{}");

        assertThat(results).allMatch(MessageSendResult::success);
        assertThat(results.get(0).providerMessageId()).isEqualTo("G4V-ALL-OK");
    }

    /**
     * 응답을 못 읽어도 <b>성공을 실패로 뒤집지 않는다.</b>
     *
     * <p>발송은 이미 일어났다. 파싱 실패를 발송 실패로 기록하면 나중에 재시도 워커가 <b>이미 간 문자를 다시 보낸다.</b>
     */
    @Test
    void doesNotFlipToFailureWhenResponseIsUnparseable() {
        List<MessageSendResult> results =
                mapper.map(List.of(message("01011112222")), "not json at all", "{}");

        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(0).providerMessageId()).isNull();
    }

    /** 빈 응답도 마찬가지다. */
    @Test
    void handlesEmptyResponse() {
        assertThat(mapper.map(List.of(message("01011112222")), null, "{}").get(0).success())
                .isTrue();
    }

    /** 응답 원문은 이력에 그대로 저장돼야 한다. 나중에 원인을 찾을 유일한 근거다. */
    @Test
    void keepsRawResponseForTheRecord() {
        List<MessageSendResult> results =
                mapper.map(List.of(message("01099998888")), REAL_RESPONSE, "{\"count\":1}");

        assertThat(results.get(0).responsePayload()).contains("groupInfo");
        assertThat(results.get(0).requestPayload()).isEqualTo("{\"count\":1}");
    }
}
