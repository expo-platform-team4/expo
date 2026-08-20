package com.expo.file.entity;

/**
 * 저장소에 올라간 객체의 위치.
 *
 * <p>{@code provider}·{@code bucket} 은 {@link com.expo.file.service.FileStorage} 구현이 스스로 보고한다. 설정값을 서비스가
 * 따로 읽어 채우면 실제로 어디에 올렸는지와 어긋날 수 있다.
 */
public record StoredLocation(String provider, String bucket, String key) {}
