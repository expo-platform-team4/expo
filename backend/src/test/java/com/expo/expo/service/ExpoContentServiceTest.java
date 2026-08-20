package com.expo.expo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.converter.ExpoContentConverter;
import com.expo.expo.dto.AttachExpoImageRequest;
import com.expo.expo.entity.Expo;
import com.expo.expo.entity.ExpoImage;
import com.expo.expo.entity.ExpoImageType;
import com.expo.expo.repository.ExpoFileRepository;
import com.expo.expo.repository.ExpoImageRepository;
import com.expo.expo.repository.ExpoRepository;
import com.expo.file.entity.FileAccessLevel;
import com.expo.file.entity.FileMetadata;
import com.expo.file.entity.StoredLocation;
import com.expo.file.entity.UploadedContent;
import com.expo.file.repository.FileMetadataRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 박람회에 파일을 붙일 때의 소유자·공개범위·형식·중복 규칙을 확인한다. */
class ExpoContentServiceTest {

    private static final Long EXPO_ID = 7L;
    private static final Long HOST_ID = 4L;
    private static final Long OTHER_CLIENT_ID = 9L;
    private static final Long FILE_ID = 1L;

    private ExpoRepository expoRepository;
    private ExpoImageRepository expoImageRepository;
    private ExpoFileRepository expoFileRepository;
    private FileMetadataRepository fileMetadataRepository;
    private ExpoContentService service;

    @BeforeEach
    void setUp() {
        expoRepository = mock(ExpoRepository.class);
        expoImageRepository = mock(ExpoImageRepository.class);
        expoFileRepository = mock(ExpoFileRepository.class);
        fileMetadataRepository = mock(FileMetadataRepository.class);
        service =
                new ExpoContentService(
                        expoRepository,
                        expoImageRepository,
                        expoFileRepository,
                        fileMetadataRepository,
                        new ExpoContentConverter());

        // mock 을 먼저 다 만든 뒤에 스터빙한다. expoOwnedByHost() 안에도 when() 이 있어서
        // 바깥 when() 의 인자로 넣으면 스터빙이 중첩되어 UnfinishedStubbingException 이 난다.
        Expo expo = expoOwnedByHost();
        when(expoRepository.findById(EXPO_ID)).thenReturn(Optional.of(expo));
        when(expoImageRepository.save(any(ExpoImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(expoImageRepository.findByExpoIdAndImageType(anyLong(), any())).thenReturn(List.of());
    }

    @Test
    void attachesPublicImage() {
        givenFile(publicImage());

        assertThat(service.attachImage(EXPO_ID, HOST_ID, thumbnailRequest()).imageType())
                .isEqualTo("THUMBNAIL");
    }

    @Test
    void rejectsWhenNotHost() {
        assertThatThrownBy(() -> service.attachImage(EXPO_ID, OTHER_CLIENT_ID, thumbnailRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_EXPO_HOST);
    }

    @Test
    void rejectsPrivateFile() {
        givenFile(privatePdf());

        assertThatThrownBy(() -> service.attachImage(EXPO_ID, HOST_ID, thumbnailRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPO_CONTENT_FILE_NOT_PUBLIC);
    }

    /** 공개 등급인 EXPO_DOCUMENT 로 올린 PDF 를 이미지 자리에 붙이는 길이 남아 있어 여기서 한 번 더 막는다. */
    @Test
    void rejectsPublicNonImageFile() {
        givenFile(publicPdf());

        assertThatThrownBy(() -> service.attachImage(EXPO_ID, HOST_ID, thumbnailRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPO_CONTENT_NOT_AN_IMAGE);
    }

    @Test
    void rejectsDuplicateFile() {
        givenFile(publicImage());
        when(expoImageRepository.existsByExpoIdAndFileId(EXPO_ID, FILE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.attachImage(EXPO_ID, HOST_ID, thumbnailRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPO_CONTENT_DUPLICATE_FILE);
    }

    @Test
    void demotesPreviousThumbnailInsteadOfDeletingIt() {
        givenFile(publicImage());
        ExpoImage previous = ExpoImage.attach(EXPO_ID, 99L, ExpoImageType.THUMBNAIL, null, 0);
        when(expoImageRepository.findByExpoIdAndImageType(EXPO_ID, ExpoImageType.THUMBNAIL))
                .thenReturn(List.of(previous));

        service.attachImage(EXPO_ID, HOST_ID, thumbnailRequest());

        assertThat(previous.getImageType()).isEqualTo(ExpoImageType.DETAIL);
        verify(expoImageRepository, never()).delete(previous);
    }

    @Test
    void appendsNewImageAfterExistingOnes() {
        givenFile(publicImage());
        when(expoImageRepository.findMaxSortOrder(EXPO_ID)).thenReturn(3);

        assertThat(service.attachImage(EXPO_ID, HOST_ID, thumbnailRequest()).sortOrder())
                .isEqualTo(4);
    }

    // ---------- 도우미 ----------

    private static AttachExpoImageRequest thumbnailRequest() {
        return new AttachExpoImageRequest(FILE_ID, ExpoImageType.THUMBNAIL, "대표 이미지");
    }

    private void givenFile(FileMetadata file) {
        when(fileMetadataRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
    }

    private static FileMetadata publicImage() {
        return fileWith("image/png", FileAccessLevel.PUBLIC);
    }

    private static FileMetadata publicPdf() {
        return fileWith("application/pdf", FileAccessLevel.PUBLIC);
    }

    private static FileMetadata privatePdf() {
        return fileWith("application/pdf", FileAccessLevel.PRIVATE);
    }

    private static FileMetadata fileWith(String contentType, FileAccessLevel accessLevel) {
        return FileMetadata.record(
                HOST_ID,
                new StoredLocation("MEMORY", "in-memory", "key"),
                new UploadedContent("f", contentType, 1L, "checksum", accessLevel));
    }

    /**
     * {@code Expo} 는 생성자가 {@code protected} 이고 테스트용 팩토리가 없다.
     *
     * <p>이 테스트가 {@code Expo} 에서 보는 것은 소유자 하나뿐이라 리플렉션으로 필드를 비트는 대신 mock 을 쓴다. 개최 승인 흐름이 들어오면
     * 그때 팩토리로 바꾼다.
     */
    private static Expo expoOwnedByHost() {
        Expo expo = mock(Expo.class);
        when(expo.getHostClientId()).thenReturn(HOST_ID);
        return expo;
    }
}
