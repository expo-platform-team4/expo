package com.expo.booth.client;

/** 토스페이먼츠 API 가 오류 응답을 반환했을 때 던지는 예외. */
public class TossApiException extends RuntimeException {

    private final String code;
    private final String rawResponse;

    public TossApiException(String code, String message, String rawResponse) {
        super(message);
        this.code = code;
        this.rawResponse = rawResponse;
    }

    public String getCode() {
        return code;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
