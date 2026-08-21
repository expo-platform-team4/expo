package com.expo.file.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 프로세스 메모리에 담아 두는 저장소. {@code app.file.storage=memory} 일 때만 빈으로 만들어진다.
 *
 * <p><b>테스트용이다.</b> {@code application-test.yml} 이 {@code spring.cloud.aws.s3.enabled=false} 라 테스트에는
 * {@code S3Client} 빈이 없다. 테스트마다 S3Mock 컨테이너를 띄우는 대신 이걸 끼운다.
 *
 * <p>재기동하면 전부 사라진다. 로컬 개발에는 쓰지 않는다 — 로컬은 S3Mock 이 파일을 실제로 남긴다.
 */
@Component
@ConditionalOnProperty(name = "app.file.storage", havingValue = "memory")
public class InMemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public String provider() {
        return "MEMORY";
    }

    @Override
    public String bucket() {
        return "in-memory";
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        objects.put(key, content.clone());
    }

    @Override
    public InputStream read(String key) {
        byte[] content = objects.get(key);
        if (content == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }
}
