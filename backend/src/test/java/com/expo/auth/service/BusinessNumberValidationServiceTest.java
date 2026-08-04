package com.expo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.auth.dto.BusinessNumberAvailabilityResponse;
import com.expo.auth.exception.InvalidBusinessNumberException;
import com.expo.auth.repository.ClientProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-API-005 사업자등록번호 사용 가능 여부 확인 서비스 단위 테스트.
 *
 * <p>DB 를 실제로 띄우지 않고 {@link ClientProfileRepository} 만 목킹한다. Repository 를 이용한 실제 저장·중복 조회는 {@code
 * AuthControllerTest} 의 통합 테스트(@SpringBootTest + H2) 에서 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class BusinessNumberValidationServiceTest {

  @Mock private ClientProfileRepository clientProfileRepository;

  @InjectMocks private BusinessNumberValidationService service;

  // ----- 정상 케이스 -----

  @Test
  void hyphenatedTestNumberReturnsAvailable() {
    when(clientProfileRepository.existsByBusinessNumber("1234567890")).thenReturn(false);

    BusinessNumberAvailabilityResponse response = service.checkAvailability("123-45-67890");

    assertThat(response.businessNumber()).isEqualTo("1234567890");
    assertThat(response.valid()).isTrue();
    assertThat(response.duplicate()).isFalse();
    assertThat(response.available()).isTrue();
    assertThat(response.message()).isEqualTo("사용 가능한 사업자등록번호입니다.");
  }

  @Test
  void plainTenDigitTestNumberReturnsAvailable() {
    when(clientProfileRepository.existsByBusinessNumber("1234567890")).thenReturn(false);

    BusinessNumberAvailabilityResponse response = service.checkAvailability("1234567890");

    assertThat(response.businessNumber()).isEqualTo("1234567890");
    assertThat(response.available()).isTrue();
  }

  @Test
  void normalizeStripsHyphens() {
    assertThat(service.normalize("123-45-67890")).isEqualTo("1234567890");
    assertThat(service.normalize("1234567890")).isEqualTo("1234567890");
    assertThat(service.normalize("111-11-11111")).isEqualTo("1111111111");
  }

  // ----- 판정 결과 (200 OK 로 응답) -----

  @Test
  void nonTestNumberReturnsNotAvailable() {
    BusinessNumberAvailabilityResponse response = service.checkAvailability("999-99-99999");

    assertThat(response.businessNumber()).isEqualTo("9999999999");
    assertThat(response.valid()).isFalse();
    assertThat(response.duplicate()).isFalse();
    assertThat(response.available()).isFalse();
    assertThat(response.message()).isEqualTo("테스트용으로 등록되지 않은 사업자등록번호입니다.");
    // 테스트 번호가 아니면 DB 조회로 넘어가지 않는다.
    verify(clientProfileRepository, never()).existsByBusinessNumber("9999999999");
  }

  @Test
  void duplicateNumberReturnsNotAvailable() {
    when(clientProfileRepository.existsByBusinessNumber("1234567890")).thenReturn(true);

    BusinessNumberAvailabilityResponse response = service.checkAvailability("1234567890");

    assertThat(response.valid()).isTrue();
    assertThat(response.duplicate()).isTrue();
    assertThat(response.available()).isFalse();
    assertThat(response.message()).isEqualTo("이미 가입된 사업자등록번호입니다.");
  }

  // ----- 형식 오류 (400 Bad Request 로 응답) -----

  @Test
  void nullInputThrowsInvalidBusinessNumber() {
    assertThatThrownBy(() -> service.checkAvailability(null))
        .isInstanceOf(InvalidBusinessNumberException.class)
        .hasMessage("사업자등록번호를 입력해 주세요.");
  }

  @Test
  void blankInputThrowsInvalidBusinessNumber() {
    assertThatThrownBy(() -> service.checkAvailability("   "))
        .isInstanceOf(InvalidBusinessNumberException.class)
        .hasMessage("사업자등록번호를 입력해 주세요.");
  }

  @Test
  void nineDigitsThrowsInvalidBusinessNumber() {
    assertThatThrownBy(() -> service.checkAvailability("123456789"))
        .isInstanceOf(InvalidBusinessNumberException.class)
        .hasMessage("사업자등록번호 형식이 올바르지 않습니다.");
  }

  @Test
  void elevenDigitsThrowsInvalidBusinessNumber() {
    assertThatThrownBy(() -> service.checkAvailability("12345678901"))
        .isInstanceOf(InvalidBusinessNumberException.class)
        .hasMessage("사업자등록번호 형식이 올바르지 않습니다.");
  }

  @Test
  void misplacedHyphenThrowsInvalidBusinessNumber() {
    // 규격은 3-2-5. 아래는 4-2-4 등 잘못된 분리.
    assertThatThrownBy(() -> service.checkAvailability("1234-56-7890"))
        .isInstanceOf(InvalidBusinessNumberException.class);
    assertThatThrownBy(() -> service.checkAvailability("12-345-67890"))
        .isInstanceOf(InvalidBusinessNumberException.class);
  }

  @Test
  void nonDigitInputThrowsInvalidBusinessNumber() {
    assertThatThrownBy(() -> service.checkAvailability("12a-45-67890"))
        .isInstanceOf(InvalidBusinessNumberException.class);
    assertThatThrownBy(() -> service.checkAvailability("123 45 67890"))
        .isInstanceOf(InvalidBusinessNumberException.class);
    assertThatThrownBy(() -> service.checkAvailability("123.45.67890"))
        .isInstanceOf(InvalidBusinessNumberException.class);
  }
}
