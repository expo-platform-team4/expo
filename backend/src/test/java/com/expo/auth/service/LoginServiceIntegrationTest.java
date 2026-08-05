package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.auth.dto.LoginRequest;
import com.expo.auth.entity.User;
import com.expo.auth.repository.RefreshTokenRepository;
import com.expo.auth.repository.UserRepository;
import com.expo.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class LoginServiceIntegrationTest {

  @Autowired private LoginService loginService;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  @Transactional
  void loginIssuesValidTokensAndUpdatesLastLogin() {
    User user =
        User.createMember(
            "login-test@espotic.com",
            passwordEncoder.encode("Test1234!"),
            "login_test_user",
            "01012345678");
    userRepository.save(user);

    LoginRequest request = new LoginRequest("login-test@espotic.com", "Test1234!");
    var response = loginService.login(request);

    assertThat(response.accessToken()).isNotBlank();
    assertThat(jwtTokenProvider.validateAccessToken(response.accessToken())).isTrue();
    assertThat(response.refreshToken()).isNotBlank();

    User updated = userRepository.findByEmail("login-test@espotic.com").orElseThrow();
    assertThat(updated.getLastLoginAt()).isNotNull();

    var tokens = refreshTokenRepository.findAll();
    assertThat(tokens).hasSize(1);
    assertThat(passwordEncoder.matches(response.refreshToken(), tokens.get(0).getTokenHash()))
        .isTrue();
  }

  @Test
  @Transactional
  void reissueAccessTokenIssuesNewAccessToken() {
    User user =
        User.createMember(
            "reissue-test@espotic.com",
            passwordEncoder.encode("Test1234!"),
            "reissue_test_user",
            "01012345678");
    userRepository.save(user);

    var loginResponse =
        loginService.login(new LoginRequest("reissue-test@espotic.com", "Test1234!"));

    var reissueResponse = loginService.reissueAccessToken(loginResponse.refreshToken());

    assertThat(reissueResponse.accessToken()).isNotBlank();
    assertThat(jwtTokenProvider.validateAccessToken(reissueResponse.accessToken())).isTrue();

    var storedToken = refreshTokenRepository.findAll().get(0);
    assertThat(storedToken.getLastUsedAt()).isNotNull();
  }
}
