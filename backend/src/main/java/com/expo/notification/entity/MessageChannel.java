package com.expo.notification.entity;

/**
 * 발송 시도 한 건이 실제로 사용한 채널.
 *
 * <p>{@link NotificationChannel} 과 따로 두는 이유는 둘이다.
 *
 * <ul>
 *   <li>{@code IN_APP} 이 없다. 외부 발송 대행사를 거치지 않아 이 이력이 생기지 않는다
 *   <li>한 알림의 시도마다 채널이 달라질 수 있다. Solapi 는 알림톡 실패 시 SMS 로 대체발송하는 것이 표준 동작이라, 알림 단위 채널
 *       하나로는 "1차 알림톡, 2차 SMS" 를 표현할 수 없다
 * </ul>
 */
public enum MessageChannel {
    KAKAO, // 카카오 알림톡
    SMS, // 문자 (Solapi)
    EMAIL // 이메일
}
