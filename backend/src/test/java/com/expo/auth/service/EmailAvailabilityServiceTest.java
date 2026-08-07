package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.expo.auth.dto.EmailAvailabilityResponse;
import com.expo.auth.exception.InvalidEmailException;
import com.expo.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** A-API-003 이메일 사용 가능 여부 확인 서비스 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class EmailAvailabilityServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private EmailAvailabilityService service;

    @Test
    void availableEmailReturnsAvailable() {
        when(userRepository.existsByEmail("member@espotic.com")).thenReturn(false);

        EmailAvailabilityResponse response = service.checkAvailability("member@espotic.com");

        assertThat(response.email()).isEqualTo("member@espotic.com");
        assertThat(response.valid()).isTrue();
        assertThat(response.duplicate()).isFalse();
        assertThat(response.available()).isTrue();
        assertThat(response.message()).isEqualTo("사용 가능한 이메일입니다.");
    }

    @Test
    void trimmedEmailIsChecked() {
        when(userRepository.existsByEmail("member@espotic.com")).thenReturn(false);

        EmailAvailabilityResponse response = service.checkAvailability("  member@espotic.com  ");

        assertThat(response.email()).isEqualTo("member@espotic.com");
        assertThat(response.available()).isTrue();
    }

    @Test
    void duplicateEmailReturnsNotAvailable() {
        when(userRepository.existsByEmail("dup@espotic.com")).thenReturn(true);

        EmailAvailabilityResponse response = service.checkAvailability("dup@espotic.com");

        assertThat(response.valid()).isTrue();
        assertThat(response.duplicate()).isTrue();
        assertThat(response.available()).isFalse();
        assertThat(response.message()).isEqualTo("이미 사용 중인 이메일입니다.");
    }

    @Test
    void nullEmailThrowsInvalidEmail() {
        assertThatThrownBy(() -> service.checkAvailability(null))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("이메일을 입력해 주세요.");
    }

    @Test
    void blankEmailThrowsInvalidEmail() {
        assertThatThrownBy(() -> service.checkAvailability("   "))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("이메일을 입력해 주세요.");
    }

    @Test
    void invalidFormatThrowsInvalidEmail() {
        assertThatThrownBy(() -> service.checkAvailability("invalid-email"))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("올바른 이메일 형식이 아닙니다.");
    }

    @Test
    void tooLongEmailThrowsInvalidEmail() {
        String longLocal = "a".repeat(250);
        assertThatThrownBy(() -> service.checkAvailability(longLocal + "@espotic.com"))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("이메일은 255자 이하여야 합니다.");
    }
}
