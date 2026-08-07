package com.expo.checkin.config;

import com.expo.checkin.service.QrTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link QrTokenProperties} 를 빈으로 등록한다. */
@Configuration
@EnableConfigurationProperties(QrTokenProperties.class)
public class QrTokenConfig {}
