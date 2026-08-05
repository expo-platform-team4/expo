package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.Role;
import com.expo.auth.converter.ClientProfileConverter;
import com.expo.auth.dto.ClientSignupRequest;
import com.expo.auth.dto.ClientSignupResponse;
import com.expo.auth.entity.ClientProfile;
import com.expo.auth.entity.User;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.exception.InvalidBusinessNumberException;
import com.expo.auth.repository.ClientProfileRepository;
import com.expo.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceClientSignupTest {

  @Mock private UserRepository userRepository;
  @Mock private ClientProfileRepository clientProfileRepository;
  @Mock private ClientProfileConverter clientProfileConverter;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private BusinessNumberValidationService businessNumberValidationService;

  @InjectMocks private AuthService authService;

  private ClientSignupRequest validRequest() {
    return new ClientSignupRequest(
        "client@espotic.com",
        "Test1234!",
        "Test1234!",
        "espotic_manager",
        "01012345678",
        "주식회사 에스포틱",
        "123-45-67890",
        true,
        true,
        false);
  }

  @Test
  void clientSignupSucceedsWithNormalizedBusinessNumber() {
    ClientSignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
    when(businessNumberValidationService.validateForClientSignup(request.businessNumber()))
        .thenReturn("1234567890");
    when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(clientProfileRepository.save(any(ClientProfile.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(clientProfileConverter.toClientSignupResponse(any(User.class), any(ClientProfile.class)))
        .thenReturn(
            new ClientSignupResponse(
                1L, Role.CLIENT, request.email(), request.companyName(), "클라이언트 회원가입이 완료되었습니다."));

    ClientSignupResponse response = authService.clientSignup(request);

    assertThat(response.role()).isEqualTo(Role.CLIENT);
    assertThat(response.email()).isEqualTo(request.email());
    verify(businessNumberValidationService).validateForClientSignup(request.businessNumber());
    verify(clientProfileRepository).save(any(ClientProfile.class));
  }

  @Test
  void clientSignupRejectsNonTestBusinessNumber() {
    ClientSignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
    when(businessNumberValidationService.validateForClientSignup(request.businessNumber()))
        .thenThrow(new InvalidBusinessNumberException("테스트용으로 등록되지 않은 사업자등록번호입니다."));

    assertThatThrownBy(() -> authService.clientSignup(request))
        .isInstanceOf(InvalidBusinessNumberException.class);

    verify(userRepository, never()).save(any(User.class));
    verify(clientProfileRepository, never()).save(any(ClientProfile.class));
  }

  @Test
  void clientSignupRejectsDuplicateBusinessNumber() {
    ClientSignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
    when(businessNumberValidationService.validateForClientSignup(request.businessNumber()))
        .thenThrow(new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER));

    assertThatThrownBy(() -> authService.clientSignup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_BUSINESS_NUMBER);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void clientSignupRejectsDuplicateEmail() {
    ClientSignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(true);

    assertThatThrownBy(() -> authService.clientSignup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

    verify(businessNumberValidationService, never()).validateForClientSignup(any());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void clientSignupRejectsDuplicateNickname() {
    ClientSignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

    assertThatThrownBy(() -> authService.clientSignup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

    verify(businessNumberValidationService, never()).validateForClientSignup(any());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void clientSignupRejectsPasswordMismatch() {
    ClientSignupRequest request =
        new ClientSignupRequest(
            "client@espotic.com",
            "Test1234!",
            "Different1!",
            "espotic_manager",
            "01012345678",
            "주식회사 에스포틱",
            "123-45-67890",
            true,
            true,
            false);

    assertThatThrownBy(() -> authService.clientSignup(request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

    verify(userRepository, never()).existsByEmail(any());
  }

  @Test
  void clientSignupValidatesWithoutCallingAvailabilityApi() {
    ClientSignupRequest request = validRequest();

    when(userRepository.existsByEmail(request.email())).thenReturn(false);
    when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
    when(businessNumberValidationService.validateForClientSignup(request.businessNumber()))
        .thenReturn("1234567890");
    when(passwordEncoder.encode(request.password())).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    when(clientProfileRepository.save(any(ClientProfile.class))).thenAnswer(i -> i.getArgument(0));
    when(clientProfileConverter.toClientSignupResponse(any(), any()))
        .thenReturn(
            new ClientSignupResponse(
                1L, Role.CLIENT, request.email(), request.companyName(), "ok"));

    authService.clientSignup(request);

    verify(businessNumberValidationService).validateForClientSignup("123-45-67890");
    verify(businessNumberValidationService, never()).checkAvailability(any());
  }
}
