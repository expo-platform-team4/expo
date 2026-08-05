package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.expo.auth.dto.NicknameAvailabilityResponse;
import com.expo.auth.exception.InvalidNicknameException;
import com.expo.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** A-API-004 닉네임 사용 가능 여부 확인 서비스 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class NicknameAvailabilityServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private NicknameAvailabilityService service;

  @Test
  void availableNicknameReturnsAvailable() {
    when(userRepository.existsByNickname("expo_member")).thenReturn(false);

    NicknameAvailabilityResponse response = service.checkAvailability("expo_member");

    assertThat(response.nickname()).isEqualTo("expo_member");
    assertThat(response.valid()).isTrue();
    assertThat(response.duplicate()).isFalse();
    assertThat(response.available()).isTrue();
    assertThat(response.message()).isEqualTo("사용 가능한 닉네임입니다.");
  }

  @Test
  void trimmedNicknameIsChecked() {
    when(userRepository.existsByNickname("expo_member")).thenReturn(false);

    NicknameAvailabilityResponse response = service.checkAvailability("  expo_member  ");

    assertThat(response.nickname()).isEqualTo("expo_member");
    assertThat(response.available()).isTrue();
  }

  @Test
  void duplicateNicknameReturnsNotAvailable() {
    when(userRepository.existsByNickname("dup_nick")).thenReturn(true);

    NicknameAvailabilityResponse response = service.checkAvailability("dup_nick");

    assertThat(response.valid()).isTrue();
    assertThat(response.duplicate()).isTrue();
    assertThat(response.available()).isFalse();
    assertThat(response.message()).isEqualTo("이미 사용 중인 닉네임입니다.");
  }

  @Test
  void nullNicknameThrowsInvalidNickname() {
    assertThatThrownBy(() -> service.checkAvailability(null))
        .isInstanceOf(InvalidNicknameException.class)
        .hasMessage("닉네임은 필수입니다.");
  }

  @Test
  void blankNicknameThrowsInvalidNickname() {
    assertThatThrownBy(() -> service.checkAvailability("   "))
        .isInstanceOf(InvalidNicknameException.class)
        .hasMessage("닉네임은 필수입니다.");
  }

  @Test
  void oneCharNicknameThrowsInvalidNickname() {
    assertThatThrownBy(() -> service.checkAvailability("a"))
        .isInstanceOf(InvalidNicknameException.class)
        .hasMessage("닉네임은 2자 이상 50자 이하여야 합니다.");
  }

  @Test
  void tooLongNicknameThrowsInvalidNickname() {
    assertThatThrownBy(() -> service.checkAvailability("a".repeat(51)))
        .isInstanceOf(InvalidNicknameException.class)
        .hasMessage("닉네임은 2자 이상 50자 이하여야 합니다.");
  }
}
