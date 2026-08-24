package com.expo.file.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.file.entity.FileMetadata;
import com.expo.file.entity.FilePurpose;
import com.expo.file.entity.StoredLocation;
import com.expo.file.entity.UploadedContent;
import com.expo.file.repository.FileMetadataRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드·조회·삭제 (이슈 #93).
 *
 * <p>파일을 <b>쓰는</b> 도메인(박람회 이미지, 부스 콘텐츠, 정산 리포트)은 여기서 받은 {@code fileId} 를 자기 테이블에 저장하기만 한다.
 * 저장소와 직접 이야기하지 않는다.
 *
 * <h2>권한</h2>
 *
 * 파일마다 {@link com.expo.file.entity.FileAccessLevel} 이 붙어 있고 업로드 용도가 그 값을 정한다. 공개 파일은 인증 없이 나가고,
 * 비공개 파일은 업로더 본인과 관리자만 받을 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileStorage fileStorage;
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageKeyGenerator storageKeyGenerator;

    /**
     * 올린다. 저장소에 객체를 먼저 넣고 그 결과를 메타데이터로 기록한다.
     *
     * <p>순서를 뒤집지 않는다 — 행을 먼저 만들면 업로드가 실패했을 때 가리킬 객체가 없는 행이 남는다. 이 순서에서 최악은 참조되지 않는 객체 하나이고,
     * 그건 나중에 정리할 수 있다.
     */
    @Transactional
    public FileMetadata upload(Long uploaderUserId, FilePurpose purpose, MultipartFile file) {
        byte[] content = validatedContentOf(purpose, file);

        String key =
                storageKeyGenerator.generate(purpose, file.getOriginalFilename(), Instant.now());
        fileStorage.put(key, content, file.getContentType());

        FileMetadata saved =
                fileMetadataRepository.save(
                        FileMetadata.record(
                                uploaderUserId,
                                new StoredLocation(
                                        fileStorage.provider(), fileStorage.bucket(), key),
                                new UploadedContent(
                                        originalFilenameOf(file),
                                        file.getContentType(),
                                        file.getSize(),
                                        sha256(content),
                                        purpose.getAccessLevel())));

        log.info(
                "파일 업로드 fileId={} purpose={} size={} uploaderUserId={}",
                saved.getId(),
                purpose,
                saved.getFileSize(),
                uploaderUserId);
        return saved;
    }

    /**
     * 내려받는다.
     *
     * <p><b>권한이 없으면 404 다</b> (403 이 아니다). 403 은 "그 파일은 있는데 네 것이 아니다" 를 알려 주는 셈이라, ID 를 훑어 남의
     * 파일 존재 여부를 확인할 수 있게 된다.
     *
     * @param requesterUserId 비로그인 요청이면 {@code null}
     * @param admin 관리자는 비공개 파일도 받을 수 있다
     */
    @Transactional(readOnly = true)
    public FileDownload download(Long fileId, Long requesterUserId, boolean admin) {
        FileMetadata metadata = readableOrThrow(fileId, requesterUserId, admin);
        return new FileDownload(metadata, fileStorage.read(metadata.getStorageKey()));
    }

    /** 메타데이터만 본다. 권한 규칙은 {@link #download} 와 같다. */
    @Transactional(readOnly = true)
    public FileMetadata get(Long fileId, Long requesterUserId, boolean admin) {
        return readableOrThrow(fileId, requesterUserId, admin);
    }

    /**
     * 논리 삭제한다. 업로더 본인과 관리자만 할 수 있다.
     *
     * <p><b>저장소의 객체는 지우지 않는다.</b> 같은 {@code file_id} 를 여러 행이 참조할 수 있어(예: 박람회 이미지와 배너가 같은 파일을 쓰는
     * 경우) 여기서 지우면 남의 화면이 깨진다. 실제 객체 정리는 참조가 0인지 확인하는 별도 배치의 일이다.
     */
    @Transactional
    public void delete(Long fileId, Long requesterUserId, boolean admin) {
        FileMetadata metadata = readableOrThrow(fileId, requesterUserId, admin);
        if (!admin && !metadata.isUploadedBy(requesterUserId)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        metadata.markDeleted();
        log.info("파일 삭제 fileId={} requesterUserId={}", fileId, requesterUserId);
    }

    // ---------- 내부 ----------

    private FileMetadata readableOrThrow(Long fileId, Long requesterUserId, boolean admin) {
        FileMetadata metadata =
                fileMetadataRepository
                        .findById(fileId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // 삭제·격리된 파일은 "없는 것" 으로 다룬다.
        if (!metadata.isDownloadable()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (metadata.isPublic() || admin || metadata.isUploadedBy(requesterUserId)) {
            return metadata;
        }
        throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }

    /** 용도별 규칙(빈 파일·크기·형식)을 확인하고 내용을 읽어 돌려준다. */
    private static byte[] validatedContentOf(FilePurpose purpose, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > purpose.getMaxSizeBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!purpose.allows(file.getContentType())) {
            throw new BusinessException(ErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_READ_FAILED);
        }
    }

    /**
     * 원본 파일명에서 경로를 떼어낸다.
     *
     * <p>브라우저에 따라 {@code C:\Users\...\poster.png} 처럼 전체 경로를 보내는 경우가 있다. 그대로 두면 컬럼(255자)을 넘길 수도
     * 있고, 나중에 이 값을 다운로드 파일명으로 쓸 때 경로가 새어 나간다.
     */
    private static String originalFilenameOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String bare = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        return bare.isBlank() ? "unnamed" : bare;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JVM 이 반드시 제공한다. 여기 오면 런타임이 깨진 것이다.
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);
        }
    }
}
