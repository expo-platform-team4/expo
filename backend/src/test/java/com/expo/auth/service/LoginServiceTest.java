package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.Role;
import com.expo.auth.dto.LoginRequest;
import com.expo.auth.dto.LoginResponse;
import com.expo.auth.entity.AccountStatus;
import com.expo.auth.entity.RefreshToken;
import com.expo.auth.entity.User;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.repository.RefreshTokenRepository;
import com.expo.auth.repository.UserRepository;
import com.expo.jwt.JwtProperties;
import com.expo.jwt.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks private LoginService loginService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser =
                User.createMember(
                        "member@espotic.com", "encoded-password", "expo_member", "01012345678");
        ReflectionTestUtils.setField(activeUser, "id", 1L);
        when(jwtProperties.getAccessTokenExpireMinutes()).thenReturn(30);
        when(jwtProperties.getRefreshTokenExpireDays()).thenReturn(14);
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        LoginRequest request = new LoginRequest("member@espotic.com", "Test1234!");

        when(userRepository.findByEmail("member@espotic.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Test1234!", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L, Role.MEMBER)).thenReturn("access-token");
        when(passwordEncoder.encode(any())).thenReturn("refresh-hash");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = loginService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.accessTokenExpiresInMinutes()).isEqualTo(30);
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("member@espotic.com");
        assertThat(response.role()).isEqualTo(Role.MEMBER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("unknown@espotic.com")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                loginService.login(
                                        new LoginRequest("unknown@espotic.com", "Test1234!")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByEmail("member@espotic.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                loginService.login(
                                        new LoginRequest("member@espotic.com", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginRejectsWithdrawnAccount() {
        ReflectionTestUtils.setField(activeUser, "accountStatus", AccountStatus.WITHDRAWN);
        when(userRepository.findByEmail("member@espotic.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(
                        () ->
                                loginService.login(
                                        new LoginRequest("member@espotic.com", "Test1234!")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.WITHDRAWN_ACCOUNT);
    }
}
