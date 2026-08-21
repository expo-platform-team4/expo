package com.expo.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 권한 경계 오류가 4xx 안에서 올바른 자리에 서 있는지 못박아 둔다.
 *
 * <p>{@link ErrorCode#NOT_EXPO_HOST} 는 원래 400 이었다(이슈 #71). 이 프로젝트는 <b>주최사와 참여 기업이 같은
 * {@code CLIENT} 역할</b>을 쓰기 때문에 {@code SecurityConfig} 의 {@code hasAnyRole} 로는 둘을 구분하지 못하고, 이
 * 코드가 사실상 유일한 권한 경계 신호다. 400 으로 돌아가면 프런트는 "입력을 고쳐 다시 보내라" 로 안내하게 되고, 모니터링에서도 권한 위반 시도가 일반
 * 입력 오류에 묻힌다.
 *
 * <p>enum 상수 한 줄이라 조용히 되돌아가기 쉬워서 여기서 막는다.
 */
class PermissionErrorStatusTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notExpoHostIsForbidden() {
        assertThat(ErrorCode.NOT_EXPO_HOST.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void notExpoHostReachesTheClientAs403() {
        var response =
                handler.handleBusinessException(new BusinessException(ErrorCode.NOT_EXPO_HOST));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.NOT_EXPO_HOST.getMessage());
    }

    /** 인증은 됐지만 역할이 모자란 경우와 같은 자리에 있어야 한다 (PR #122 에서 추가된 코드). */
    @Test
    void permissionCodesShareTheSameStatus() {
        assertThat(ErrorCode.NOT_EXPO_HOST.getStatus())
                .isEqualTo(ErrorCode.ACCESS_DENIED.getStatus());
    }
}
