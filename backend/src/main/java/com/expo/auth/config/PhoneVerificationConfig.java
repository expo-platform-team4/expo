package com.expo.auth.config;

import com.expo.auth.service.PhoneVerificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

//휴대폰 인증 yaml 설정을 읽는 PhoneVerificationProperties를 Spring Bean으로 켜 주는 스위치” 파일
@Configuration
@EnableConfigurationProperties(PhoneVerificationProperties.class)
public class PhoneVerificationConfig {}
