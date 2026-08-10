package com.expo.booth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link TossPaymentProperties} 를 빈으로 등록한다. */
@Configuration
@EnableConfigurationProperties(TossPaymentProperties.class)
public class TossPaymentConfig {}
