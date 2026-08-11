package com.expo.notification.config;

import com.expo.notification.service.SolapiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link SolapiProperties} 를 빈으로 등록한다. */
@Configuration
@EnableConfigurationProperties(SolapiProperties.class)
public class SmsConfig {}
