package com.expo.file.service;

import java.io.InputStream;

/**
 * 객체 저장소. 파일의 실제 바이트를 넣고 빼는 일만 한다.
 *
 * <p>구현이 둘이고 {@code app.file.storage} 로 고른다.
 *
 * <ul>
 *   <li>{@code s3} (기본) — {@link S3FileStorage}. 로컬에서는 docker-compose 의 S3Mock 을 가리킨다
 *   <li>{@code memory} — {@link InMemoryFileStorage}. 테스트 전용
 * </ul>
 *
 * <p><b>메타데이터를 모른다.</b> 누가 올렸는지, 무엇에 쓰는지, 누가 볼 수 있는지는 전부 {@code file_metadata} 와 {@link FileService}
 * 의 몫이다. 여기는 키와 바이트만 다룬다.
 */
public interface FileStorage {

    /** {@code file_metadata.storage_provider} 에 그대로 기록될 값. */
    String provider();

    /** {@code file_metadata.bucket_name} 에 그대로 기록될 값. */
    String bucket();

    void put(String key, byte[] content, String contentType);

    /**
     * 읽기용 스트림을 연다. <b>호출한 쪽이 닫는다.</b>
     *
     * @throws com.expo.common.exception.BusinessException 키에 해당하는 객체가 없을 때
     */
    InputStream read(String key);

    /** 없는 키를 지우는 것은 오류가 아니다. */
    void delete(String key);
}
