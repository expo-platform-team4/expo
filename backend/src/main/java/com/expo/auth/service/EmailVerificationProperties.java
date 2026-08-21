package com.expo.auth.service;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 이메일 본인인증 설정 ({@code app.email-verification}). */
@ConfigurationProperties(prefix = "app.email-verification")
@Validated
public class EmailVerificationProperties {

    /** 인증코드 유효 시간(분). 기본 5분 — 휴대폰(3분)보다 여유를 둔다. 메일 수신이 SMS보다 느릴 수 있어서다. */
    @Positive private int codeExpireMinutes = 5;

    public int getCodeExpireMinutes() {
        return codeExpireMinutes;
    }

    public void setCodeExpireMinutes(int codeExpireMinutes) {
        this.codeExpireMinutes = codeExpireMinutes;
    }
}
