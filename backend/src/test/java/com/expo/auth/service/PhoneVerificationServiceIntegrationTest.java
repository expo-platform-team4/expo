package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
import com.expo.auth.repository.PhoneVerificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class PhoneVerificationServiceIntegrationTest {

    @Autowired private PhoneVerificationService phoneVerificationService;
    @Autowired private PhoneVerificationRepository phoneVerificationRepository;

    @Test
    @Transactional
    void requestVerificationPersistsRecordAndExpiresPreviousRequest() {
        var firstResponse = phoneVerificationService.requestVerification("01011112222");

        phoneVerificationService.requestVerification("01011112222");

        PhoneVerification expired =
                phoneVerificationRepository.findById(firstResponse.verificationId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(PhoneVerificationStatus.EXPIRED);

        var active =
                phoneVerificationRepository.findAll().stream()
                        .filter(
                                v ->
                                        v.getPhoneNumber().equals("01011112222")
                                                && v.getStatus()
                                                        == PhoneVerificationStatus.REQUESTED)
                        .toList();
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getId()).isNotEqualTo(firstResponse.verificationId());
    }

    @Test
    @Transactional
    void confirmVerificationWithTestCodeMarksVerified() {
        var requestResponse = phoneVerificationService.requestVerification("01099998888");

        var confirmResponse =
                phoneVerificationService.confirmVerification(
                        requestResponse.verificationId(), "123456");

        assertThat(confirmResponse.signupVerificationToken()).isNotBlank();

        PhoneVerification verified =
                phoneVerificationRepository
                        .findById(requestResponse.verificationId())
                        .orElseThrow();
        assertThat(verified.getStatus()).isEqualTo(PhoneVerificationStatus.VERIFIED);
        assertThat(verified.getVerifiedAt()).isNotNull();
    }
}
