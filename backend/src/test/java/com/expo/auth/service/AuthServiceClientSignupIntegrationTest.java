package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.auth.Role;
import com.expo.auth.dto.ClientSignupRequest;
import com.expo.auth.entity.ClientProfile;
import com.expo.auth.entity.User;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.exception.InvalidBusinessNumberException;
import com.expo.auth.repository.ClientProfileRepository;
import com.expo.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceClientSignupIntegrationTest {

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private ClientProfileRepository clientProfileRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private ClientSignupRequest buildRequest(
      String email, String nickname, String companyName, String businessNumber) {
    return new ClientSignupRequest(
        email,
        "Test1234!",
        "Test1234!",
        nickname,
        "01012345678",
        companyName,
        businessNumber,
        true,
        true,
        false);
  }

  @Test
  void clientSignupStoresNormalizedBusinessNumberAndClientRole() {
    authService.clientSignup(
        buildRequest("client1@test.com", "manager1", "주식회사 에스포틱", "123-45-67890"));

    User user =
        userRepository.findAll().stream()
            .filter(u -> u.getEmail().equals("client1@test.com"))
            .findFirst()
            .orElseThrow();
    ClientProfile profile = clientProfileRepository.findById(user.getId()).orElseThrow();

    assertThat(user.getRole()).isEqualTo(Role.CLIENT);
    assertThat(user.getNickname()).isEqualTo("manager1");
    assertThat(user.getPhoneNumber()).isEqualTo("01012345678");
    assertThat(user.getPasswordHash()).isNotEqualTo("Test1234!");
    assertThat(passwordEncoder.matches("Test1234!", user.getPasswordHash())).isTrue();
    assertThat(profile.getBusinessNumber()).isEqualTo("1234567890");
    assertThat(profile.getCompanyName()).isEqualTo("주식회사 에스포틱");
  }

  @Test
  void clientSignupWithPlainDigitsStoresTenDigitBusinessNumber() {
    authService.clientSignup(buildRequest("client2@test.com", "manager2", "두번째 회사", "1111111111"));

    ClientProfile profile =
        clientProfileRepository.findAll().stream()
            .filter(p -> p.getBusinessNumber().equals("1111111111"))
            .findFirst()
            .orElseThrow();
    assertThat(profile.getBusinessNumber()).isEqualTo("1111111111");
  }

  @Test
  void clientSignupFailsForDuplicateBusinessNumberWithoutSavingUser() {
    authService.clientSignup(buildRequest("dup-bn-1@test.com", "manager3", "첫번째 회사", "2222222222"));

    assertThatThrownBy(
            () ->
                authService.clientSignup(
                    buildRequest("dup-bn-2@test.com", "manager4", "두번째 회사", "222-22-22222")))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_BUSINESS_NUMBER);

    assertThat(userRepository.existsByEmail("dup-bn-2@test.com")).isFalse();
    assertThat(clientProfileRepository.existsByBusinessNumber("2222222222")).isTrue();
  }

  @Test
  void clientSignupFailsForInvalidBusinessNumberWithoutSaving() {
    assertThatThrownBy(
            () ->
                authService.clientSignup(
                    buildRequest("invalid@test.com", "manager5", "실패 회사", "999-99-99999")))
        .isInstanceOf(InvalidBusinessNumberException.class);

    assertThat(userRepository.existsByEmail("invalid@test.com")).isFalse();
    assertThat(clientProfileRepository.existsByBusinessNumber("9999999999")).isFalse();
  }
}
