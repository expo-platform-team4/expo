package com.expo.auth.config;

import com.expo.auth.service.EmailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link EmailProperties} 를 빈으로 등록한다. */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {}
