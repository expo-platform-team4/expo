package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.Role;
import com.expo.auth.converter.UserConverter;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.entity.User;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
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
  @Mock private UserConverter userConverter;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthService authService;

  @Test
  void signupCreatesMember() {
    SignupRequest request = new SignupRequest("member@example.com", "password123", "expo_member");

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
  void signupRejectsDuplicateEmail() {
    SignupRequest request = new SignupRequest("dup@example.com", "password123", "nickname");

    when(userRepository.existsByEmail(request.email())).thenReturn(true);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void signupRejectsDuplicateNickname() {
    SignupRequest request = new SignupRequest("new@example.com", "password123", "taken");

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

    verify(userRepository, never()).save(any(User.class));
  }
}
