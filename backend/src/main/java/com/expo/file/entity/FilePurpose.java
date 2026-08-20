package com.expo.file.entity;

import java.util.Set;

/**
 * 파일을 무엇에 쓰는가. 업로드 요청이 이 값을 함께 보내고, 여기에 딸린 규칙이 <b>허용 형식·최대 크기·공개 범위·저장 경로</b>를 한꺼번에 결정한다.
 *
 * <p><b>이 값은 저장하지 않는다.</b> {@code file_metadata} 에 용도 컬럼이 없고, 파일이 실제로 어디 쓰이는지는 참조하는 쪽 테이블
 * ({@code expo_images.image_type} 등)이 이미 갖고 있다. 여기서는 업로드 시점에 규칙을 고르는 데만 쓰고, 그 결과인 {@link
 * FileAccessLevel} 과 저장 키만 남는다.
 *
 * <p>용도를 늘릴 때 규칙을 빠뜨릴 수 없게 생성자에서 전부 받는다.
 */
public enum FilePurpose {

    /** 회원·클라이언트 프로필 이미지. 스키마 문서가 정한 5MB·JPEG/PNG/WEBP 를 그대로 따른다. */
    PROFILE_IMAGE("profile-image", FileAccessLevel.PUBLIC, 5, ImageTypes.WEB_IMAGE),

    /** 박람회 대표·상세·갤러리 이미지 ({@code expo_images}). */
    EXPO_IMAGE("expo-image", FileAccessLevel.PUBLIC, 10, ImageTypes.WEB_IMAGE),

    /** 박람회 소개 PDF·카탈로그·리플렛 ({@code expo_files}). 방문객에게 보여 주는 자료라 공개다. */
    EXPO_DOCUMENT("expo-document", FileAccessLevel.PUBLIC, 20, ImageTypes.DOCUMENT),

    /** 부스 로고·대표 이미지·갤러리 ({@code booth_contents}, {@code booth_content_files}). */
    BOOTH_IMAGE("booth-image", FileAccessLevel.PUBLIC, 10, ImageTypes.WEB_IMAGE),

    /** 부스 카탈로그·리플렛 ({@code booth_content_files}). */
    BOOTH_DOCUMENT("booth-document", FileAccessLevel.PUBLIC, 20, ImageTypes.DOCUMENT),

    /** 광고 배너 이미지 ({@code banner_applications}, {@code banners}). */
    BANNER_IMAGE("banner-image", FileAccessLevel.PUBLIC, 5, ImageTypes.WEB_IMAGE),

    /** 장소·전시관·구역 배치도 ({@code virtual_venues.map_file_id} 등). 도면이라 PDF 도 받는다. */
    VENUE_LAYOUT("venue-layout", FileAccessLevel.PUBLIC, 10, ImageTypes.IMAGE_OR_PDF),

    /**
     * 정산 리포트 ({@code settlement_reports}). <b>유일한 비공개 용도다</b> — 주최사의 매출과 수수료가 들어 있어 업로더 본인과 관리자만
     * 받을 수 있어야 한다.
     */
    SETTLEMENT_REPORT("settlement-report", FileAccessLevel.PRIVATE, 20, ImageTypes.REPORT);

    private static final long MEGABYTE = 1024L * 1024L;

    private final String keyPrefix;
    private final FileAccessLevel accessLevel;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    FilePurpose(
            String keyPrefix,
            FileAccessLevel accessLevel,
            int maxSizeMegabytes,
            Set<String> allowedContentTypes) {
        this.keyPrefix = keyPrefix;
        this.accessLevel = accessLevel;
        this.maxSizeBytes = maxSizeMegabytes * MEGABYTE;
        this.allowedContentTypes = allowedContentTypes;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public FileAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public boolean allows(String contentType) {
        return contentType != null && allowedContentTypes.contains(contentType.toLowerCase());
    }

    /**
     * 허용 형식 묶음.
     *
     * <p>enum 상수의 생성자 인자로 쓰이므로 enum 자신의 static 필드로는 둘 수 없다 — 상수가 static 초기화보다 먼저 만들어져 {@code null}
     * 이 넘어온다. 중첩 클래스에 두면 그 클래스가 먼저 초기화된다.
     */
    private static final class ImageTypes {

        private static final Set<String> WEB_IMAGE =
                Set.of("image/jpeg", "image/png", "image/webp");

        private static final Set<String> DOCUMENT = Set.of("application/pdf");

        private static final Set<String> IMAGE_OR_PDF =
                Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

        private static final Set<String> REPORT =
                Set.of(
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private ImageTypes() {}
    }
}
