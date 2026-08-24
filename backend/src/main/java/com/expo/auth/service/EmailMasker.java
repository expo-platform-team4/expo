package com.expo.auth.service;

import org.springframework.stereotype.Component;

/**
 * 이메일 주소를 가린다. {@code abcdef@example.com} → {@code ab***@example.com}
 *
 * <p>{@code docs/logging.md} 156행 — "이메일·전화번호는 꼭 필요하면 {@code ab***@example.com} 형태로
 * 마스킹한다"를 그대로 따른다. 도메인은 가리지 않는다 — 어느 메일 서비스를 쓰는지는 식별 위험이 낮고,
 * 발송 실패 로그를 볼 때 도메인이 있어야 원인(오탈자, 존재하지 않는 도메인 등)을 가늠할 수 있다.
 */
@Component
public class EmailMasker {

    private static final int VISIBLE_PREFIX_LENGTH = 2;
    private static final String MASK = "***";

    public String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return MASK;
        }
        String localPart = email.substring(0, at);
        String domain = email.substring(at);
        String visible =
                localPart.length() <= VISIBLE_PREFIX_LENGTH
                        ? localPart
                        : localPart.substring(0, VISIBLE_PREFIX_LENGTH);
        return visible + MASK + domain;
    }
}
