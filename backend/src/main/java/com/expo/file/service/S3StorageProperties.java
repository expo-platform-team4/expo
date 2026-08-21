package com.expo.file.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 객체 저장소 설정 ({@code app.s3}).
 *
 * <p>엔드포인트·자격증명·리전은 여기가 아니라 {@code spring.cloud.aws.*} 가 갖는다 — Spring Cloud AWS 가 그 값으로 {@code
 * S3Client} 를 만들어 준다. 우리가 따로 아는 것은 버킷 이름뿐이다.
 */
@ConfigurationProperties(prefix = "app.s3")
public class S3StorageProperties {

    /** 버킷명. 로컬은 S3Mock 이 기동 시 만들어 둔 {@code expo-local} 이다. */
    private String bucket;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
