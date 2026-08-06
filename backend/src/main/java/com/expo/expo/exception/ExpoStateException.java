package com.expo.expo.exception;

/**
 * 박람회 상태 전이 규칙 위반 시 발생 (예: 승인 후 직접 수정 시도 — 희-EXPO-06)
 * GlobalExceptionHandler에서 409 CONFLICT 또는 400 BAD_REQUEST로 매핑 권장.
 */
public class ExpoStateException extends RuntimeException {

    public ExpoStateException(String message) {
        super(message);
    }
}
