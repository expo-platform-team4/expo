package com.expo.expo.exception;

/**
 * 박람회 도메인 상태 전이 규칙 위반 시 발생 (예: 승인 후 직접 수정 시도 — 희-EXPO-06)
 * 전역 예외 핸들러에서 409 CONFLICT 매핑 권장.
 */
public class ExpoStateException extends RuntimeException {

    public ExpoStateException(String message) {
        super(message);
    }
}
