package com.expo.auth.converter;

import com.expo.auth.dto.ClientSignupResponse;
import com.expo.auth.entity.ClientProfile;
import com.expo.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ClientProfileConverter {

  public ClientSignupResponse toClientSignupResponse(User user, ClientProfile profile) {
    return new ClientSignupResponse(
        user.getId(),
        user.getRole(),
        user.getEmail(),
        profile.getCompanyName(),
        "클라이언트 회원가입이 완료되었습니다.");
  }
}
