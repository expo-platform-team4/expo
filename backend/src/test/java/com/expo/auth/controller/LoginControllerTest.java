package com.expo.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expo.auth.Role;
import com.expo.auth.dto.LoginResponse;
import com.expo.auth.dto.TokenReissueResponse;
import com.expo.auth.exception.BusinessException;
import com.expo.auth.exception.ErrorCode;
import com.expo.auth.service.LoginService;
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
class LoginControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LoginService loginService;

  @Test
  void loginReturnsOkWithTokens() throws Exception {
    Instant refreshExpires = Instant.now().plusSeconds(86400);
    when(loginService.login(any()))
        .thenReturn(
            new LoginResponse(
                "access-token",
                30,
                "refresh-token",
                refreshExpires,
                1L,
                "member@espotic.com",
                "expo_member",
                Role.MEMBER));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@espotic.com",
                      "password": "Test1234!"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.role").value("MEMBER"));
  }

  @Test
  void loginWithInvalidCredentialsReturnsUnauthorized() throws Exception {
    when(loginService.login(any()))
        .thenThrow(new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@espotic.com",
                      "password": "wrong"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void loginWithoutTokenIsAllowed() throws Exception {
    when(loginService.login(any()))
        .thenReturn(
            new LoginResponse(
                "access-token",
                30,
                "refresh-token",
                Instant.now().plusSeconds(86400),
                1L,
                "member@espotic.com",
                "expo_member",
                Role.MEMBER));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"member@espotic.com\",\"password\":\"Test1234!\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void reissueAccessTokenReturnsOk() throws Exception {
    when(loginService.reissueAccessToken(any()))
        .thenReturn(new TokenReissueResponse("new-access-token", 30));

    mockMvc
        .perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "refreshToken": "valid-refresh-token"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
  }

  @Test
  void reissueAccessTokenWithoutLoginTokenIsAllowed() throws Exception {
    when(loginService.reissueAccessToken(any()))
        .thenReturn(new TokenReissueResponse("new-access-token", 30));

    mockMvc
        .perform(
            post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"valid-refresh-token\"}"))
        .andExpect(status().isOk());
  }
}
