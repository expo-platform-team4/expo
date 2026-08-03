package com.expo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 애플리케이션 진입점.
 *
 * <p>이 클래스가 {@code com.expo} 에 있으므로 컴포넌트 스캔과 MyBatis 매퍼 스캔이 {@code com.expo} 이하 전체 도메인 패키지를 자동으로
 * 훑는다. 도메인을 추가할 때 스캔 설정을 따로 손댈 필요가 없다.
 */
@SpringBootApplication
public class ExpoApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExpoApplication.class, args);
  }
}
