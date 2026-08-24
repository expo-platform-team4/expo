package com.expo.file.config;

import com.expo.file.service.S3StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** {@link S3StorageProperties} 를 빈으로 등록한다. */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class FileStorageConfig {}
