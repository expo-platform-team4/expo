package com.expo.notification.dto;

/**
 * 알림 수신자.
 *
 * <p>회원이면 {@code userId} 가 있고, 비회원 주문이면 {@code null} 이다. <b>회원이라도 번호가 없을 수 있다</b> — 소셜 로그인
 * 회원은 {@code users.phone_number} 가 비어 있다.
 *
 * @param userId {@code notifications.recipient_user_id} 로 들어간다. 비회원이면 {@code null}
 * @param phoneNumber 수신 번호. 없으면 {@code null}
 */
public record NotificationRecipient(Long userId, String phoneNumber) {

    /** 보낼 수 있는 상태인가. 번호가 없으면 시도 자체를 하지 않는다. */
    public boolean reachable() {
        return phoneNumber != null && !phoneNumber.isBlank();
    }

    /** 번호를 모르는 수신자. 회원 여부만 아는 경우다. */
    public static NotificationRecipient unreachable(Long userId) {
        return new NotificationRecipient(userId, null);
    }
}
