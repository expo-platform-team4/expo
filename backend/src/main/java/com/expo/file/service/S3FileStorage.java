package com.expo.file.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3 호환 저장소에 넣고 뺀다. {@code app.file.storage=s3} 일 때만 빈으로 만들어진다(기본).
 *
 * <p>로컬에서는 docker-compose 의 Adobe S3Mock 을 가리킨다. 같은 S3 API 라 코드가 갈리지 않는다.
 *
 * <p><b>presigned URL 을 쓰지 않는다.</b> 브라우저가 저장소로 직접 붙는 대신 애플리케이션이 바이트를 중계한다. S3Mock 이 presigned
 * URL 의 서명·만료·메서드를 하나도 검증하지 않아서, 그 방식은 로컬이 통과해도 운영에서 처음 터진다 —
 * {@code docs/s3-presigned-url.md} 에 정리되어 있다. 우리 파일은 최대 20MB 라 중계로 충분하다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.file.storage", havingValue = "s3", matchIfMissing = true)
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorage(S3Client s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getBucket();
    }

    @Override
    public String provider() {
        return "S3";
    }

    @Override
    public String bucket() {
        return bucket;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            log.error("파일 업로드 실패 bucket={} key={}", bucket, key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public InputStream read(String key) {
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            // 메타데이터 행은 있는데 객체가 없다. 정상 흐름에서는 나올 수 없어 로그를 남긴다.
            log.error("메타데이터에 있는 객체가 저장소에 없다 bucket={} key={}", bucket, key, e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        } catch (S3Exception e) {
            log.error("파일 조회 실패 bucket={} key={}", bucket, key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.error("파일 삭제 실패 bucket={} key={}", bucket, key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }
}
