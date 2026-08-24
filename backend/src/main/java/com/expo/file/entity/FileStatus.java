package com.expo.file.entity;

/** {@code file_metadata.file_status} 의 CHECK 제약과 값이 같아야 한다. */
public enum FileStatus {

    /** 정상. 조회·다운로드가 가능하다. */
    ACTIVE,

    /** 논리 삭제. 참조가 끊긴 파일이며 실제 객체 정리는 별도 배치가 맡는다. */
    DELETED,

    /** 격리. 악성으로 판정되었거나 검사 중이라 내주지 않는다. */
    QUARANTINED
}
