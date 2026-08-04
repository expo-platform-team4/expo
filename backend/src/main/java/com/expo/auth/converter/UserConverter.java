package com.expo.auth.converter;

import com.expo.auth.dto.SignupResponse;
import com.expo.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

  public SignupResponse toSignupResponse(User user) {
    return new SignupResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
  }
}
