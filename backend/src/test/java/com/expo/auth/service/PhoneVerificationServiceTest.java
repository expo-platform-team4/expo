package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.dto.PhoneVerificationCreateResponse;
import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
import com.expo.auth.exception.InvalidPhoneNumberException;
import com.expo.auth.repository.PhoneVerificationRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneVerificationServiceTest {

  @Mock private PhoneVerificationRepository phoneVerificationRepository;
  @Mock private PhoneVerificationProperties properties;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private PhoneVerificationService phoneVerificationService;

  @Test
  void normalizeStripsHyphens() {
    assertThat(phoneVerificationService.normalize("010-1234-5678")).isEqualTo("01012345678");
  }

  @Test
  void normalizeRejectsInvalidFormat() {
    assertThatThrownBy(() -> phoneVerificationService.normalize("02012345678"))
        .isInstanceOf(InvalidPhoneNumberException.class);
  }

  @Test
  void requestVerificationExpiresPreviousAndSavesRequestedRecord() {
    when(properties.getCodeExpireMinutes()).thenReturn(3);
    when(properties.getTestVerificationCode()).thenReturn("123456");
    when(passwordEncoder.encode(any())).thenReturn("token-hash");
    when(phoneVerificationRepository.save(any(PhoneVerification.class)))
        .thenAnswer(
            invocation -> {
              PhoneVerification verification = invocation.getArgument(0);
              ReflectionTestUtils.setField(verification, "id", 1L);
              return verification;
            });

    PhoneVerificationCreateResponse response =
        phoneVerificationService.requestVerification("01012345678");

    assertThat(response.phoneNumber()).isEqualTo("01012345678");
    assertThat(response.verificationId()).isEqualTo(1L);
    assertThat(response.expiresAt()).isAfter(Instant.now());
    assertThat(response.message()).isEqualTo("인증번호가 발송되었습니다.");

    verify(phoneVerificationRepository)
        .expireRequestedByPhoneNumber(
            eq("01012345678"),
            eq(PhoneVerificationStatus.REQUESTED),
            eq(PhoneVerificationStatus.EXPIRED));

    ArgumentCaptor<PhoneVerification> captor = ArgumentCaptor.forClass(PhoneVerification.class);
    verify(phoneVerificationRepository).save(captor.capture());
    PhoneVerification saved = captor.getValue();
    assertThat(saved.getPhoneNumber()).isEqualTo("01012345678");
    assertThat(saved.getStatus()).isEqualTo(PhoneVerificationStatus.REQUESTED);
    assertThat(saved.getVerificationTokenHash()).isEqualTo("token-hash");
  }
}
