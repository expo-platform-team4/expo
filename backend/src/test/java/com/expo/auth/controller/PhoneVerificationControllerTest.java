package com.expo.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expo.auth.dto.PhoneVerificationConfirmResponse;
import com.expo.auth.dto.PhoneVerificationCreateResponse;
import com.expo.auth.exception.InvalidPhoneNumberException;
import com.expo.auth.service.PhoneVerificationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PhoneVerificationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PhoneVerificationService phoneVerificationService;

  @Test
  void requestVerificationReturnsCreated() throws Exception {
    Instant expiresAt = Instant.now().plusSeconds(180);
    when(phoneVerificationService.requestVerification(any()))
        .thenReturn(
            new PhoneVerificationCreateResponse(1L, "01012345678", expiresAt, "인증번호가 발송되었습니다."));

    mockMvc
        .perform(
            post("/api/auth/phone-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "phoneNumber": "01012345678"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.verificationId").value(1))
        .andExpect(jsonPath("$.data.phoneNumber").value("01012345678"));
  }

  @Test
  void requestVerificationWithInvalidPhoneReturnsBadRequest() throws Exception {
    when(phoneVerificationService.requestVerification(any()))
        .thenThrow(new InvalidPhoneNumberException("휴대폰 번호 형식이 올바르지 않습니다."));

    mockMvc
        .perform(
            post("/api/auth/phone-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "phoneNumber": "02012345678"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void requestVerificationWithoutTokenIsAllowed() throws Exception {
    when(phoneVerificationService.requestVerification(any()))
        .thenReturn(
            new PhoneVerificationCreateResponse(
                1L, "01012345678", Instant.now().plusSeconds(180), "인증번호가 발송되었습니다."));

    mockMvc
        .perform(
            post("/api/auth/phone-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"01012345678\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void confirmVerificationReturnsOk() throws Exception {
    Instant verifiedAt = Instant.now();
    when(phoneVerificationService.confirmVerification(any(), any()))
        .thenReturn(
            new PhoneVerificationConfirmResponse(
                1L, "01012345678", verifiedAt, "signup-token-uuid", "휴대폰 인증이 완료되었습니다."));

    mockMvc
        .perform(
            post("/api/auth/phone-verifications/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "verificationId": 1,
                      "verificationCode": "123456"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.signupVerificationToken").value("signup-token-uuid"));
  }

  @Test
  void confirmVerificationWithoutTokenIsAllowed() throws Exception {
    when(phoneVerificationService.confirmVerification(any(), any()))
        .thenReturn(
            new PhoneVerificationConfirmResponse(
                1L, "01012345678", Instant.now(), "signup-token-uuid", "휴대폰 인증이 완료되었습니다."));

    mockMvc
        .perform(
            post("/api/auth/phone-verifications/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"verificationId\":1,\"verificationCode\":\"123456\"}"))
        .andExpect(status().isOk());
  }
}
