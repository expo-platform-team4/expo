package com.expo.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.file.entity.FileAccessLevel;
import com.expo.file.entity.FileMetadata;
import com.expo.file.entity.FilePurpose;
import com.expo.file.repository.FileMetadataRepository;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 업로드 검증과 내려받기 권한을 확인한다.
 *
 * <p>저장소는 mock 이 아니라 {@link InMemoryFileStorage} 를 그대로 쓴다. 실제 구현이라 넣은 바이트가 그대로 나오는지까지 같이 확인된다.
 */
class FileServiceTest {

    private static final Long UPLOADER_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;
    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    private InMemoryFileStorage storage;
    private FileMetadataRepository repository;
    private FileService service;

    @BeforeEach
    void setUp() {
        storage = new InMemoryFileStorage();
        repository = mock(FileMetadataRepository.class);
        service = new FileService(storage, repository, new StorageKeyGenerator());
        when(repository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- 업로드 ----------

    @Test
    void storesUploadedBytesUnchanged() throws IOException {
        FileMetadata saved = upload(FilePurpose.EXPO_IMAGE, "poster.png", "image/png", PNG_BYTES);

        assertThat(storage.read(saved.getStorageKey()).readAllBytes()).isEqualTo(PNG_BYTES);
        assertThat(saved.getFileSize()).isEqualTo(PNG_BYTES.length);
        assertThat(saved.getChecksum()).hasSize(64);
    }

    @Test
    void purposeDecidesAccessLevel() {
        FileMetadata image = upload(FilePurpose.EXPO_IMAGE, "poster.png", "image/png", PNG_BYTES);
        FileMetadata report =
                upload(FilePurpose.SETTLEMENT_REPORT, "report.pdf", "application/pdf", PNG_BYTES);

        assertThat(image.getAccessLevel()).isEqualTo(FileAccessLevel.PUBLIC);
        assertThat(report.getAccessLevel()).isEqualTo(FileAccessLevel.PRIVATE);
    }

    @Test
    void rejectsDisallowedContentType() {
        assertThatThrownBy(
                        () ->
                                upload(
                                        FilePurpose.PROFILE_IMAGE,
                                        "report.pdf",
                                        "application/pdf",
                                        PNG_BYTES))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED);
    }

    @Test
    void appliesSizeLimitPerPurpose() {
        // 프로필은 5MB, 박람회 이미지는 10MB 다. 같은 6MB 가 한쪽만 통과해야 한다.
        byte[] sixMegabytes = new byte[6 * 1024 * 1024];

        assertThatThrownBy(
                        () ->
                                upload(
                                        FilePurpose.PROFILE_IMAGE,
                                        "big.png",
                                        "image/png",
                                        sixMegabytes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);

        assertThat(upload(FilePurpose.EXPO_IMAGE, "big.png", "image/png", sixMegabytes))
                .isNotNull();
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(
                        () -> upload(FilePurpose.EXPO_IMAGE, "empty.png", "image/png", new byte[0]))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_EMPTY);
    }

    @Test
    void stripsPathFromOriginalFilename() {
        FileMetadata saved =
                upload(FilePurpose.EXPO_IMAGE, "C:\\Users\\me\\poster.png", "image/png", PNG_BYTES);

        assertThat(saved.getOriginalFilename()).isEqualTo("poster.png");
    }

    // ---------- 내려받기 권한 ----------

    @Test
    void servesPublicFileToAnonymous() {
        givenStored(upload(FilePurpose.EXPO_IMAGE, "poster.png", "image/png", PNG_BYTES));

        assertThat(service.download(1L, null, false).metadata()).isNotNull();
    }

    @Test
    void hidesPrivateFileFromOtherUser() {
        givenStored(upload(FilePurpose.SETTLEMENT_REPORT, "r.pdf", "application/pdf", PNG_BYTES));

        assertThatThrownBy(() -> service.download(1L, OTHER_USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void hidesPrivateFileFromAnonymous() {
        givenStored(upload(FilePurpose.SETTLEMENT_REPORT, "r.pdf", "application/pdf", PNG_BYTES));

        assertThatThrownBy(() -> service.download(1L, null, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void servesPrivateFileToUploaderAndAdmin() {
        givenStored(upload(FilePurpose.SETTLEMENT_REPORT, "r.pdf", "application/pdf", PNG_BYTES));

        assertThat(service.download(1L, UPLOADER_ID, false).metadata()).isNotNull();
        assertThat(service.download(1L, OTHER_USER_ID, true).metadata()).isNotNull();
    }

    @Test
    void hidesDeletedFileEvenWhenPublic() {
        FileMetadata metadata = upload(FilePurpose.EXPO_IMAGE, "p.png", "image/png", PNG_BYTES);
        metadata.markDeleted();
        givenStored(metadata);

        assertThatThrownBy(() -> service.download(1L, UPLOADER_ID, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void rejectsDeleteByOtherUser() {
        givenStored(upload(FilePurpose.EXPO_IMAGE, "p.png", "image/png", PNG_BYTES));

        assertThatThrownBy(() -> service.delete(1L, OTHER_USER_ID, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void deleteIsLogicalAndKeepsStoredObject() throws IOException {
        FileMetadata metadata = upload(FilePurpose.EXPO_IMAGE, "p.png", "image/png", PNG_BYTES);
        givenStored(metadata);

        service.delete(1L, UPLOADER_ID, false);

        assertThat(metadata.isDownloadable()).isFalse();
        assertThat(storage.read(metadata.getStorageKey()).readAllBytes()).isEqualTo(PNG_BYTES);
    }

    // ---------- 도우미 ----------

    private FileMetadata upload(
            FilePurpose purpose, String filename, String contentType, byte[] content) {
        return service.upload(
                UPLOADER_ID,
                purpose,
                new MockMultipartFile("file", filename, contentType, content));
    }

    private void givenStored(FileMetadata metadata) {
        when(repository.findById(1L)).thenReturn(Optional.of(metadata));
    }
}
