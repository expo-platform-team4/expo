package com.expo.file.entity;

/**
 * 파일을 누가 볼 수 있는가. {@code file_metadata.access_level} 의 CHECK 제약과 값이 같아야 한다.
 *
 * <p>{@link FileStatus} 와 혼동하지 않는다 — 저쪽은 파일이 살아 있느냐이고 이쪽은 누가 볼 수 있느냐다. 둘 다 통과해야 파일이 나간다.
 */
public enum FileAccessLevel {

    /**
     * 인증 없이 내준다. 박람회 대표 이미지·팜플렛처럼 비회원도 보는 파일이다.
     *
     * <p>프론트 인증이 {@code Authorization: Bearer} 헤더라 {@code <img src>} 는 토큰을 실을 수 없다. 화면에 그려야 하는 이미지는
     * 이 등급이어야 한다.
     */
    PUBLIC,

    /** 업로더 본인과 관리자만. 정산 리포트처럼 남이 보면 안 되는 파일이다. */
    PRIVATE
}
