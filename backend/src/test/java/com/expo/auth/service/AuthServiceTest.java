package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.Role;
import com.expo.auth.converter.ClientProfileConverter;
import com.expo.auth.converter.UserConverter;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.entity.User;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.repository.ClientProfileRepository;
import com.expo.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ClientProfileRepository clientProfileRepository;
  @Mock private UserConverter userConverter;
  @Mock private ClientProfileConverter clientProfileConverter;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private BusinessNumberValidationService businessNumberValidationService;

  @InjectMocks private AuthService authService;

  private SignupRequest validRequest() {
    return new SignupRequest(
        "member@espotic.com",
        "Test1234!",
        "Test1234!",
        "expo_member",
        "01012345678",
        true,
        true,
        false);
  }

  @Test
  void signupCreatesMember() {
    SignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
    when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userConverter.toSignupResponse(any(User.class)))
        .thenReturn(new SignupResponse(1L, request.email(), request.nickname(), Role.MEMBER));

    SignupResponse response = authService.signup(request);

    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo(request.email());
    assertThat(response.nickname()).isEqualTo(request.nickname());
    assertThat(response.role()).isEqualTo(Role.MEMBER);
    verify(userRepository).save(any(User.class));
  }

  @Test
  void signupRejectsPasswordMismatch() {
    SignupRequest request =
        new SignupRequest(
            "member@espotic.com",
            "Test1234!",
            "Different1!",
            "expo_member",
            "01012345678",
            true,
            true,
            false);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

    verify(userRepository, never()).existsByEmail(any());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void signupRejectsDuplicateEmail() {
    SignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(true);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void signupRejectsDuplicateNickname() {
    SignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

    verify(userRepository, never()).save(any(User.class));
  }
}
