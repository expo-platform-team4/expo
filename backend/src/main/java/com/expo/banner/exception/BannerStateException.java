package com.expo.banner.exception;

/**
 * 배너 신청 도메인의 상태 규칙 위반 예외 (V1: banner_requests).
 *
 * 승인/반려 가능 상태가 아닌데 처리하려는 경우, 노출 기간 검증 실패 등
 * BannerRequest 엔티티의 비즈니스 규칙 위반 시 사용한다.
 * expo 모듈의 ExpoStateException과 동일한 역할을 하되, 배너 모듈이
 * expo 모듈에 의존하지 않도록 독립적으로 둔다.
 */
public class BannerStateException extends RuntimeException {

    public BannerStateException(String message) {
        super(message);
    }

    public BannerStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
