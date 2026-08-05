package com.expo.auth.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// application.yml 설정을 읽어 들이는 설정 클래스(Properties)
/** 휴대폰 본인인증 MVP 설정 ({@code app.phone-verification}). */
@ConfigurationProperties(prefix = "app.phone-verification")
@Validated
public class PhoneVerificationProperties {

    /** 인증번호 유효 시간(분). 기본 3분. */
    @Positive private int codeExpireMinutes = 3;

    /**
     * MVP 테스트용 고정 인증번호. 외부 SMS API 미연동 시 이 값으로 검증 통과.
     *
     * <p>검증 API 구현 시 사용한다.
     */
    // 빈 값 금지 (application.yml / env 에 값이 없으면 앱 시작 시 검증 실패).
    @NotBlank
    // 6자리 숫자만 허용 (예: 123456). SMS 연동 전 MVP 테스트용.
    @Pattern(regexp = "^\\d{6}$")
    // 기본값 123456. application.yml의 test-verification-code 또는 PHONE_VERIFICATION_TEST_CODE로 변경 가능.
    private String testVerificationCode = "123456";

    // PhoneVerificationService에서 expiresAt 계산 시 사용 (now + N분).
    public int getCodeExpireMinutes() {
        return codeExpireMinutes;
    }

    // Spring이 application.yml의 code-expire-minutes 값을 여기에 넣어 준다.
    public void setCodeExpireMinutes(int codeExpireMinutes) {
        this.codeExpireMinutes = codeExpireMinutes;
    }

    // MVP: 실제 SMS 없이 이 코드로 인증 통과. DEBUG 로그·검증 API에서 사용.
    public String getTestVerificationCode() {
        return testVerificationCode;
    }

    // Spring이 application.yml / env의 test-verification-code 값을 여기에 넣어 준다.
    public void setTestVerificationCode(String testVerificationCode) {
        this.testVerificationCode = testVerificationCode;
    }
}
