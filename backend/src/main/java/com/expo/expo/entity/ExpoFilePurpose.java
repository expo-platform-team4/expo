package com.expo.expo.entity;

/** {@code expo_files.file_purpose} 의 CHECK 제약과 값이 같아야 한다. */
public enum ExpoFilePurpose {

    /** 박람회 소개 PDF. */
    INTRO_PDF,

    /** 참가 업체·전시품 카탈로그. */
    CATALOG,

    /** 리플렛(팜플렛). */
    LEAFLET,

    /** 홍보 영상. */
    PROMO_VIDEO,

    /** 그 밖의 자료. */
    OTHER
}
