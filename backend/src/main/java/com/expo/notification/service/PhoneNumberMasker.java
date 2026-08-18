package com.expo.notification.service;

import org.springframework.stereotype.Component;

/**
 * 수신번호를 가린다. {@code 01012345678} → {@code 010****5678}
 *
 * <p>원래 {@link LoggingSmsSender} 안에 private 메서드로 있었다. 관리자 조회(D-API-009)가 같은 규칙을 쓰게 되면서
 * 밖으로 뺐다 — 두 곳이 각자 가리면 한쪽만 고쳐지고 다른 쪽이 원문을 흘린다.
 *
 * <p>가운데를 가리는 이유는 <b>앞뒤가 식별에 쓰이기 때문</b>이다. 앞 3자리로 통신사 형식을, 뒤 4자리로 본인 확인을 한다.
 * 전부 가리면 관리자가 대사할 수 없고, 다 보여 주면 목록 하나에 수백 개의 번호가 노출된다.
 */
@Component
public class PhoneNumberMasker {

    /** 앞 3 + 뒤 4 를 남기려면 최소 7자리가 필요하다. */
    private static final int MIN_LENGTH = 7;

    private static final String FULLY_MASKED = "***";

    public String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < MIN_LENGTH) {
            return phoneNumber == null ? null : FULLY_MASKED;
        }
        return phoneNumber.substring(0, 3)
                + "****"
                + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
