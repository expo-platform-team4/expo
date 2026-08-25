package com.expo.auth.service;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 휴대폰 본인인증 설정 ({@code app.phone-verification}).
 *
 * <p>고정 테스트 인증번호 설정은 없앴다. 인증번호는 요청마다 무작위로 만들어 SMS 로만 전달한다.
 * 고정값을 남겨 두면 운영에서도 그 값이 통해, 남의 번호로 본인인증을 통과시킬 수 있다.
 *
 * <p>로컬에서는 {@code SMS_PROVIDER=logging} 이라 실제 문자 대신 로그로 확인할 수 있어,
 * 테스트용 우회 값이 없어도 개발에 지장이 없다.
 */
@ConfigurationProperties(prefix = "app.phone-verification")
@Validated
public class PhoneVerificationProperties {

    /** 인증번호 유효 시간(분). 기본 3분. */
    @Positive private int codeExpireMinutes = 3;

    /**
     * 같은 번호로 인증번호를 다시 보낼 수 있기까지의 최소 간격(초). 기본 60초.
     *
     * <p>이 엔드포인트는 로그인 없이 열려 있고 SMS 는 건당 과금이다. 간격 제한이 없으면
     * 아무나 남의 번호로 문자를 무한히 발송시킬 수 있고, 그 비용은 우리가 낸다.
     */
    @Positive private int resendCooldownSeconds = 60;

    /**
     * 인증 성공 시 발급하는 회원가입용 토큰의 유효 시간(분). 기본 30분.
     *
     * <p>인증번호 유효시간(3분)과 다르다. 인증을 마친 뒤 나머지 가입 항목을 채우는 시간이라
     * 넉넉해야 하지만, 무기한이면 발급된 토큰이 영영 살아 있게 된다. 이메일 인증과 같은 값이다.
     */
    @Positive private int signupTokenExpireMinutes = 30;

    public int getCodeExpireMinutes() {
        return codeExpireMinutes;
    }

    public void setCodeExpireMinutes(int codeExpireMinutes) {
        this.codeExpireMinutes = codeExpireMinutes;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public int getSignupTokenExpireMinutes() {
        return signupTokenExpireMinutes;
    }

    public void setSignupTokenExpireMinutes(int signupTokenExpireMinutes) {
        this.signupTokenExpireMinutes = signupTokenExpireMinutes;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }
}
