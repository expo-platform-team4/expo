package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
import com.expo.auth.repository.PhoneVerificationRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.service.SmsSender;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 휴대폰 본인인증의 규칙을 못박는다.
 *
 * <p>가장 중요한 둘은 <b>인증번호가 매번 달라진다</b>는 것과 <b>재요청 간격이 강제된다</b>는 것이다. 전자는 고정
 * 인증번호로 아무 번호나 통과시키던 구멍을 막고, 후자는 로그인 없이 열린 이 API 로 남의 번호에 문자를
 * 무한 발송시키는 것을 막는다 — SMS 는 건당 과금이라 비용이 곧 피해다.
 *
 * <p>{@code passwordEncoder} 만 진짜(BCrypt)를 쓴다. 해시와 검증이 실제로 맞물리는지가 시험 대상이라
 * mock 으로 바꾸면 확인할 것이 없어진다.
 *
 * <p>문자 발송이 {@code afterCommit} 에 걸려 있어, 트랜잭션이 없는 단위 테스트에서는
 * {@link TransactionSynchronizationManager} 를 직접 열고 {@link #commit()} 으로 커밋을 흉내 낸다.
 */
class PhoneVerificationServiceTest {

    private static final String PHONE = "010-1234-5678";
    private static final String NORMALIZED = "01012345678";
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    private final PhoneVerificationRepository repository =
            Mockito.mock(PhoneVerificationRepository.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PhoneVerificationProperties properties = new PhoneVerificationProperties();

    private PhoneVerificationService service;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        service = new PhoneVerificationService(repository, properties, passwordEncoder, smsSender);
        when(repository.save(any(PhoneVerification.class))).thenAnswer(i -> i.getArgument(0));
        when(smsSender.send(anyString(), anyString()))
                .thenReturn(MessageSendResult.accepted("mid", "{}", "{}"));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void sendsADifferentCodeEachTime() {
        when(repository.findLatestRequestedAt(NORMALIZED)).thenReturn(null);

        // 해시끼리 비교하면 안 된다 — BCrypt 는 salt 때문에 같은 원문도 매번 다른 해시가 나온다.
        // 실제로 보낸 인증번호를 봐야 한다.
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            service.requestVerification(PHONE);
            commit();
        }
        for (String text : sentTexts()) {
            codes.add(extractCode(text));
        }

        assertThat(codes).hasSizeGreaterThan(1);
    }

    @Test
    void onlyTheSentCodeMatchesTheStoredHash() {
        when(repository.findLatestRequestedAt(NORMALIZED)).thenReturn(null);
        service.requestVerification(PHONE);
        commit();

        String sentCode = extractCode(sentTexts().getLast());
        String codeHash = savedVerifications().getLast().getVerificationTokenHash();

        assertThat(passwordEncoder.matches(sentCode, codeHash)).isTrue();
        assertThat(passwordEncoder.matches(otherThan(sentCode), codeHash)).isFalse();
    }

    @Test
    void neverReturnsTheCodeInTheResponse() {
        when(repository.findLatestRequestedAt(NORMALIZED)).thenReturn(null);

        var response = service.requestVerification(PHONE);
        commit();

        assertThat(response.toString()).doesNotContain(extractCode(sentTexts().getLast()));
    }

    @Test
    void rejectsResendWithinCooldownAndSendsNothing() {
        Instant justNow = Instant.now().minusSeconds(properties.getResendCooldownSeconds() - 10L);
        when(repository.findLatestRequestedAt(NORMALIZED)).thenReturn(justNow);

        assertThatThrownBy(() -> service.requestVerification(PHONE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PHONE_VERIFICATION_TOO_FREQUENT);

        // 거부된 요청이 멀쩡한 직전 인증 건을 무효로 만들어서도 안 된다.
        verify(repository, never()).expireRequestedByPhoneNumber(anyString(), any(), any());
        verify(smsSender, never()).send(anyString(), anyString());
    }

    @Test
    void resendsOnceCooldownHasPassed() {
        Instant longAgo = Instant.now().minusSeconds(properties.getResendCooldownSeconds() + 10L);
        when(repository.findLatestRequestedAt(NORMALIZED)).thenReturn(longAgo);

        service.requestVerification(PHONE);
        commit();

        verify(smsSender).send(anyString(), anyString());
    }

    @Test
    void persistsFailedOnCodeMismatch() {
        PhoneVerification requested =
                verification(Instant.now(), Instant.now().plus(3, ChronoUnit.MINUTES));
        when(repository.findById(1L)).thenReturn(Optional.of(requested));

        assertThatThrownBy(() -> service.confirmVerification(1L, "999999"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH);

        // REQUESTED 로 남으면 같은 건에 인증번호를 무제한 추측할 수 있다.
        assertThat(requested.getStatus()).isEqualTo(PhoneVerificationStatus.FAILED);
    }

    @Test
    void persistsExpiredWhenPastExpiry() {
        PhoneVerification expired =
                verification(
                        Instant.now().minus(10, ChronoUnit.MINUTES),
                        Instant.now().minus(5, ChronoUnit.MINUTES));
        when(repository.findById(1L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.confirmVerification(1L, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PHONE_VERIFICATION_EXPIRED);

        assertThat(expired.getStatus()).isEqualTo(PhoneVerificationStatus.EXPIRED);
    }

    // ---------- 도우미 ----------

    /** 커밋을 흉내 내 {@code afterCommit} 에 걸어 둔 문자 발송을 실행시킨다. */
    private void commit() {
        for (TransactionSynchronization sync :
                TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
    }

    private java.util.List<String> sentTexts() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(smsSender, Mockito.atLeastOnce()).send(anyString(), captor.capture());
        return captor.getAllValues();
    }

    private java.util.List<PhoneVerification> savedVerifications() {
        ArgumentCaptor<PhoneVerification> captor = ArgumentCaptor.forClass(PhoneVerification.class);
        verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private PhoneVerification verification(Instant requestedAt, Instant expiresAt) {
        return PhoneVerification.createRequested(
                NORMALIZED, passwordEncoder.encode("123456"), requestedAt, expiresAt);
    }

    private String extractCode(String text) {
        Matcher matcher = SIX_DIGITS.matcher(text);
        assertThat(matcher.find()).as("문자 본문에 6자리 인증번호가 있어야 한다: %s", text).isTrue();
        return matcher.group(1);
    }

    /** 주어진 코드와 확실히 다른 6자리. 무작위 값과 우연히 같아지는 것을 피한다. */
    private String otherThan(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }
}
