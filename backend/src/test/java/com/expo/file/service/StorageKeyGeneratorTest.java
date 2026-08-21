package com.expo.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.file.entity.FilePurpose;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 객체 키가 원본 파일명을 흘리지 않고 충돌하지 않는지 확인한다. */
class StorageKeyGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-08-20T05:30:00Z");

    private final StorageKeyGenerator generator = new StorageKeyGenerator();

    @Test
    void generatesKeyFromPurposeDateAndExtension() {
        String key = generator.generate(FilePurpose.EXPO_IMAGE, "poster.png", NOW);

        assertThat(key).matches("expo-image/2026/08/20/[0-9a-f-]{36}\\.png");
    }

    @Test
    void keepsOriginalFilenameOutOfKey() {
        String key = generator.generate(FilePurpose.EXPO_IMAGE, "우리 회사 포스터.png", NOW);

        assertThat(key).doesNotContain("포스터").doesNotContain(" ");
    }

    @Test
    void generatesDistinctKeysForSameFilename() {
        String first = generator.generate(FilePurpose.EXPO_IMAGE, "poster.png", NOW);
        String second = generator.generate(FilePurpose.EXPO_IMAGE, "poster.png", NOW);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void dropsMissingOrSuspiciousExtension() {
        String noExtension = "expo-document/2026/08/20/[0-9a-f-]{36}";

        // 확장자 자리에 경로가 들어오면 키가 디렉터리를 거슬러 올라갈 수 있다
        assertThat(generator.generate(FilePurpose.EXPO_DOCUMENT, "a.pdf/../../secret", NOW))
                .matches(noExtension);
        assertThat(generator.generate(FilePurpose.EXPO_DOCUMENT, "리플렛", NOW)).matches(noExtension);
        assertThat(generator.generate(FilePurpose.EXPO_DOCUMENT, "리플렛.", NOW)).matches(noExtension);
    }

    @Test
    void lowercasesExtension() {
        assertThat(generator.generate(FilePurpose.EXPO_IMAGE, "POSTER.PNG", NOW)).endsWith(".png");
    }
}
