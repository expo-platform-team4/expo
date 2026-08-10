package com.expo.notification.entity;

/**
 * 알림을 보낼 채널.
 *
 * <p>{@link MessageChannel} 과 값이 다르다. 인앱 알림은 외부 발송 대행사를 거치지 않아 발송 이력이 생기지 않으므로 {@code
 * MESSAGE_HISTORIES} 쪽에는 {@code IN_APP} 이 없다. 하나로 합치면 안 된다.
 */
public enum NotificationChannel {
    KAKAO, // 카카오 알림톡
    SMS, // 문자 (Solapi)
    EMAIL, // 이메일
    IN_APP // 앱 내 알림. 외부 발송 없음
}
