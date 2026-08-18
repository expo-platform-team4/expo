package com.expo.notification.dto;

/**
 * 대량 발송에 넣을 문자 한 통.
 *
 * @param to 수신 번호 (숫자만)
 * @param text 본문
 */
public record SmsMessage(String to, String text) {}
