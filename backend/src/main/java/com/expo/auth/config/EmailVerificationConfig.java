package com.expo.auth.config;

import com.expo.auth.service.EmailVerificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link EmailVerificationProperties} 를 빈으로 등록한다. */
@Configuration
@EnableConfigurationProperties(EmailVerificationProperties.class)
public class EmailVerificationConfig {}
