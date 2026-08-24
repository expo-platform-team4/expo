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

    /**
     * 인증 완료 후 발급하는 가입토큰(signupVerificationToken) 자체의 유효 시간(분). 코드 확인이 끝나면
     * {@code expires_at}(코드 만료시각)은 더 이상 검사되지 않으므로, 가입 절차를 마칠 시간을 별도로
     * 준다 — 비밀번호 재설정 토큰(30분)과 같은 값을 기본으로 쓴다.
     */
    @Positive private int signupTokenExpireMinutes = 30;

    public int getCodeExpireMinutes() {
        return codeExpireMinutes;
    }

    public void setCodeExpireMinutes(int codeExpireMinutes) {
        this.codeExpireMinutes = codeExpireMinutes;
    }

    public int getSignupTokenExpireMinutes() {
        return signupTokenExpireMinutes;
    }

    public void setSignupTokenExpireMinutes(int signupTokenExpireMinutes) {
        this.signupTokenExpireMinutes = signupTokenExpireMinutes;
    }
}
