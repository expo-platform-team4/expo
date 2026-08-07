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
import com.expo.auth.dto.EmailAvailabilityResponse;

import com.expo.auth.dto.NicknameAvailabilityResponse;

import com.expo.auth.dto.SignupResponse;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.exception.InvalidBusinessNumberException;
import com.expo.auth.exception.InvalidEmailException;

import com.expo.auth.exception.InvalidNicknameException;
import com.expo.auth.service.AuthService;
import com.expo.auth.service.BusinessNumberValidationService;
import com.expo.auth.service.EmailAvailabilityService;
import com.expo.auth.service.NicknameAvailabilityService;

import com.expo.auth.service.AuthService;
import com.expo.auth.service.BusinessNumberValidationService;
import com.expo.auth.service.EmailAvailabilityService;

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
    @MockitoBean private EmailAvailabilityService emailAvailabilityService;

    @MockitoBean private NicknameAvailabilityService nicknameAvailabilityService;

    @MockitoBean private BusinessNumberValidationService businessNumberValidationService;

    // ----- 회원가입 (A-API-001) -----

    @Test
    void signupWithoutTokenIsAllowed() throws Exception {
        when(authService.signup(any()))
                .thenReturn(
                        new SignupResponse(1L, "member@espotic.com", "expo_member", Role.MEMBER));

        mockMvc.perform(
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
        mockMvc.perform(
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
        mockMvc.perform(
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
        mockMvc.perform(
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

    // ----- 이메일 사용 가능 여부 (A-API-003) -----

    @Test
    void emailAvailabilityWithoutTokenIsAllowed() throws Exception {
        when(emailAvailabilityService.checkAvailability("member@espotic.com"))
                .thenReturn(EmailAvailabilityResponse.available("member@espotic.com"));

        mockMvc.perform(get("/api/auth/email-availability").param("email", "member@espotic.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("member@espotic.com"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void emailAvailabilityForDuplicateReturnsUnavailable() throws Exception {
        when(emailAvailabilityService.checkAvailability("dup@espotic.com"))
                .thenReturn(EmailAvailabilityResponse.duplicate("dup@espotic.com"));

        mockMvc.perform(get("/api/auth/email-availability").param("email", "dup@espotic.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    void emailAvailabilityWithInvalidFormatReturnsBadRequest() throws Exception {
        when(emailAvailabilityService.checkAvailability("invalid-email"))
                .thenThrow(new InvalidEmailException("올바른 이메일 형식이 아닙니다."));

        mockMvc.perform(get("/api/auth/email-availability").param("email", "invalid-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("올바른 이메일 형식이 아닙니다."));
    }

    @Test
    void emailAvailabilityWithBlankReturnsBadRequest() throws Exception {
        when(emailAvailabilityService.checkAvailability(""))
                .thenThrow(new InvalidEmailException("이메일을 입력해 주세요."));

        mockMvc.perform(get("/api/auth/email-availability").param("email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일을 입력해 주세요."));
    }
