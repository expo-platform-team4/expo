package com.expo.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expo.auth.Role;
import com.expo.auth.dto.BusinessNumberAvailabilityResponse;
import com.expo.auth.dto.ClientSignupResponse;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.exception.InvalidBusinessNumberException;
import com.expo.auth.service.AuthService;
import com.expo.auth.service.BusinessNumberValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;
  @MockitoBean private BusinessNumberValidationService businessNumberValidationService;

  // ----- 회원가입 (A-API-001) -----

  @Test
  void signupWithoutTokenIsAllowed() throws Exception {
    when(authService.signup(any()))
        .thenReturn(new SignupResponse(1L, "member@espotic.com", "expo_member", Role.MEMBER));

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@espotic.com",
                      "password": "Test1234!",
                      "passwordConfirm": "Test1234!",
                      "nickname": "expo_member",
                      "phoneNumber": "01012345678",
                      "serviceTermsAgreed": true,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.email").value("member@espotic.com"))
        .andExpect(jsonPath("$.data.nickname").value("expo_member"))
        .andExpect(jsonPath("$.data.role").value("MEMBER"));
  }

  @Test
  void signupWithInvalidEmailReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "invalid-email",
                      "password": "Test1234!",
                      "passwordConfirm": "Test1234!",
                      "nickname": "expo_member",
                      "phoneNumber": "01012345678",
                      "serviceTermsAgreed": true,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void signupWithWeakPasswordReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@espotic.com",
                      "password": "onlyletters",
                      "passwordConfirm": "onlyletters",
                      "nickname": "expo_member",
                      "phoneNumber": "01012345678",
                      "serviceTermsAgreed": true,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void signupWithoutRequiredTermsReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@espotic.com",
                      "password": "Test1234!",
                      "passwordConfirm": "Test1234!",
                      "nickname": "expo_member",
                      "phoneNumber": "01012345678",
                      "serviceTermsAgreed": false,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  // ----- 사업자등록번호 사용 가능 여부 (A-API-005) -----

  @Test
  void businessNumberAvailabilityWithoutTokenIsAllowed() throws Exception {
    when(businessNumberValidationService.checkAvailability("123-45-67890"))
        .thenReturn(BusinessNumberAvailabilityResponse.available("1234567890"));

    mockMvc
        .perform(
            get("/api/auth/business-number-availability").param("businessNumber", "123-45-67890"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.businessNumber").value("1234567890"))
        .andExpect(jsonPath("$.data.valid").value(true))
        .andExpect(jsonPath("$.data.duplicate").value(false))
        .andExpect(jsonPath("$.data.available").value(true));
  }

  @Test
  void businessNumberAvailabilityWithPlainDigitsReturnsAvailable() throws Exception {
    when(businessNumberValidationService.checkAvailability("1234567890"))
        .thenReturn(BusinessNumberAvailabilityResponse.available("1234567890"));

    mockMvc
        .perform(
            get("/api/auth/business-number-availability").param("businessNumber", "1234567890"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.available").value(true))
        .andExpect(jsonPath("$.data.businessNumber").value("1234567890"));
  }

  @Test
  void businessNumberAvailabilityForNonTestNumberReturnsUnavailable() throws Exception {
    when(businessNumberValidationService.checkAvailability("999-99-99999"))
        .thenReturn(BusinessNumberAvailabilityResponse.notTestNumber("9999999999"));

    mockMvc
        .perform(
            get("/api/auth/business-number-availability").param("businessNumber", "999-99-99999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid").value(false))
        .andExpect(jsonPath("$.data.available").value(false))
        .andExpect(jsonPath("$.data.message").value("테스트용으로 등록되지 않은 사업자등록번호입니다."));
  }

  @Test
  void businessNumberAvailabilityForDuplicateReturnsUnavailable() throws Exception {
    when(businessNumberValidationService.checkAvailability("1234567890"))
        .thenReturn(BusinessNumberAvailabilityResponse.duplicate("1234567890"));

    mockMvc
        .perform(
            get("/api/auth/business-number-availability").param("businessNumber", "1234567890"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid").value(true))
        .andExpect(jsonPath("$.data.duplicate").value(true))
        .andExpect(jsonPath("$.data.available").value(false))
        .andExpect(jsonPath("$.data.message").value("이미 가입된 사업자등록번호입니다."));
  }

  @Test
  void businessNumberAvailabilityWithBlankReturnsBadRequest() throws Exception {
    when(businessNumberValidationService.checkAvailability(""))
        .thenThrow(new InvalidBusinessNumberException("사업자등록번호를 입력해 주세요."));

    mockMvc
        .perform(get("/api/auth/business-number-availability").param("businessNumber", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("사업자등록번호를 입력해 주세요."));
  }

  @Test
  void businessNumberAvailabilityWithNineDigitsReturnsBadRequest() throws Exception {
    when(businessNumberValidationService.checkAvailability("123456789"))
        .thenThrow(new InvalidBusinessNumberException("사업자등록번호 형식이 올바르지 않습니다."));

    mockMvc
        .perform(get("/api/auth/business-number-availability").param("businessNumber", "123456789"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void businessNumberAvailabilityWithMisplacedHyphenReturnsBadRequest() throws Exception {
    when(businessNumberValidationService.checkAvailability("1234-56-7890"))
        .thenThrow(new InvalidBusinessNumberException("사업자등록번호 형식이 올바르지 않습니다."));

    mockMvc
        .perform(
            get("/api/auth/business-number-availability").param("businessNumber", "1234-56-7890"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void businessNumberAvailabilityWithNonDigitReturnsBadRequest() throws Exception {
    when(businessNumberValidationService.checkAvailability("12a-45-67890"))
        .thenThrow(new InvalidBusinessNumberException("사업자등록번호 형식이 올바르지 않습니다."));

    mockMvc
        .perform(
            get("/api/auth/business-number-availability").param("businessNumber", "12a-45-67890"))
        .andExpect(status().isBadRequest());
  }

  // ----- 클라이언트 회원가입 (A-API-002) -----

  @Test
  void clientSignupWithoutTokenIsAllowed() throws Exception {
    when(authService.clientSignup(any()))
        .thenReturn(
            new ClientSignupResponse(
                1L, Role.CLIENT, "client@espotic.com", "주식회사 에스포틱", "클라이언트 회원가입이 완료되었습니다."));

    mockMvc
        .perform(
            post("/api/auth/client-signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "client@espotic.com",
                      "password": "Test1234!",
                      "passwordConfirm": "Test1234!",
                      "nickname": "espotic_manager",
                      "phoneNumber": "01012345678",
                      "companyName": "주식회사 에스포틱",
                      "businessNumber": "123-45-67890",
                      "serviceTermsAgreed": true,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.role").value("CLIENT"))
        .andExpect(jsonPath("$.data.email").value("client@espotic.com"))
        .andExpect(jsonPath("$.data.companyName").value("주식회사 에스포틱"));
  }

  @Test
  void clientSignupWithPasswordMismatchReturnsBadRequest() throws Exception {
    when(authService.clientSignup(any()))
        .thenThrow(new BusinessException(ErrorCode.PASSWORD_MISMATCH));

    mockMvc
        .perform(
            post("/api/auth/client-signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "client@espotic.com",
                      "password": "Test1234!",
                      "passwordConfirm": "Different1!",
                      "nickname": "espotic_manager",
                      "phoneNumber": "01012345678",
                      "companyName": "주식회사 에스포틱",
                      "businessNumber": "123-45-67890",
                      "serviceTermsAgreed": true,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void clientSignupWithoutRequiredTermsReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/client-signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "client@espotic.com",
                      "password": "Test1234!",
                      "passwordConfirm": "Test1234!",
                      "nickname": "espotic_manager",
                      "phoneNumber": "01012345678",
                      "companyName": "주식회사 에스포틱",
                      "businessNumber": "123-45-67890",
                      "serviceTermsAgreed": false,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void clientSignupWithWeakPasswordReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/client-signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "client@espotic.com",
                      "password": "onlyletters",
                      "passwordConfirm": "onlyletters",
                      "nickname": "espotic_manager",
                      "phoneNumber": "01012345678",
                      "companyName": "주식회사 에스포틱",
                      "businessNumber": "123-45-67890",
                      "serviceTermsAgreed": true,
                      "privacyPolicyAgreed": true,
                      "marketingAgreed": false
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
