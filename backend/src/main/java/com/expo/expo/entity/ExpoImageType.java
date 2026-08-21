package com.expo.expo.entity;

/** {@code expo_images.image_type} 의 CHECK 제약과 값이 같아야 한다. */
public enum ExpoImageType {

    /** 목록·카드에 뜨는 대표 이미지. 박람회당 하나만 의미가 있다. */
    THUMBNAIL,

    /** 상세 화면 본문 이미지. */
    DETAIL,

    /** 갤러리. 여러 장을 순서대로 보여 준다. */
    GALLERY
}
