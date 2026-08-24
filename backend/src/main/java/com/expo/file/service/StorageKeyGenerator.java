package com.expo.file.service;

import com.expo.file.entity.FilePurpose;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 객체 키를 만든다. 형식은 {@code {용도}/{yyyy/MM/dd}/{UUID}.{확장자}} 다.
 *
 * <pre>
 * expo-image/2026/08/20/3f2b1c9e-....png
 * </pre>
 *
 * <p><b>원본 파일명을 키에 쓰지 않는다.</b> 한글·공백·경로 구분자가 섞여 들어오고, 같은 이름을 올리면 앞 파일을 덮어쓴다. 원본 이름은
 * {@code file_metadata.original_filename} 에 따로 남으므로 잃어버리지 않는다.
 *
 * <p>날짜를 끼우는 것은 한 접두어 아래 객체가 무한히 쌓이지 않게 하려는 것이다. 저장소 콘솔에서 사람이 뒤질 때도 이 편이 낫다.
 */
@Component
public class StorageKeyGenerator {

    private static final DateTimeFormatter DATE_PATH =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    /** 확장자로 허용할 문자. 이 밖의 문자가 하나라도 있으면 확장자를 통째로 버린다. */
    private static final String SAFE_EXTENSION = "[a-z0-9]{1,10}";

    public String generate(FilePurpose purpose, String originalFilename, Instant now) {
        String extension = extensionOf(originalFilename);
        return purpose.getKeyPrefix()
                + "/"
                + DATE_PATH.format(now)
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    /** 앞에 점을 붙여 돌려준다. 확장자가 없거나 수상하면 빈 문자열이다. */
    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extension.matches(SAFE_EXTENSION) ? "." + extension : "";
    }
}
