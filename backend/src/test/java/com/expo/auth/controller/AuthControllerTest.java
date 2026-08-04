package com.expo.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expo.auth.Role;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.service.AuthService;
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

  @Test
  void signupWithoutTokenIsAllowed() throws Exception {
    when(authService.signup(any()))
        .thenReturn(new SignupResponse(1L, "member@example.com", "expo_member", Role.MEMBER));

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@example.com",
                      "password": "password123",
                      "nickname": "expo_member"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(1))
        .andExpect(jsonPath("$.data.email").value("member@example.com"))
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
                      "password": "password123",
                      "nickname": "expo_member"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void signupWithShortPasswordReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "member@example.com",
                      "password": "short",
                      "nickname": "expo_member"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
