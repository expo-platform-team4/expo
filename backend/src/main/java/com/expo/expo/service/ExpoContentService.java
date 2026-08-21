package com.expo.expo.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.converter.ExpoContentConverter;
import com.expo.expo.dto.AttachExpoFileRequest;
import com.expo.expo.dto.AttachExpoImageRequest;
import com.expo.expo.dto.ExpoFileResponse;
import com.expo.expo.dto.ExpoImageResponse;
import com.expo.expo.entity.Expo;
import com.expo.expo.entity.ExpoFile;
import com.expo.expo.entity.ExpoImage;
import com.expo.expo.entity.ExpoImageType;
import com.expo.expo.repository.ExpoFileRepository;
import com.expo.expo.repository.ExpoImageRepository;
import com.expo.expo.repository.ExpoRepository;
import com.expo.file.entity.FileMetadata;
import com.expo.file.repository.FileMetadataRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 박람회 이미지·자료 붙이기 (이슈 #93).
 *
 * <p><b>파일 업로드와 연결은 두 단계다.</b> 먼저 {@code POST /api/files} 로 파일을 올려 {@code fileId} 를 받고, 그 ID 를 여기로
 * 보낸다. 한 번에 처리하지 않는 이유는 같은 파일을 여러 곳에 붙일 수 있어야 하고, 업로드가 느린 동안 연결까지 트랜잭션에 묶이면 안 되기 때문이다.
 *
 * <p>연결할 수 있는 파일은 <b>공개 파일뿐이다</b>. 비공개 파일을 붙이면 목록에는 뜨는데 이미지가 404 로 깨진다.
 */
@Service
@RequiredArgsConstructor
public class ExpoContentService {

    private final ExpoRepository expoRepository;
    private final ExpoImageRepository expoImageRepository;
    private final ExpoFileRepository expoFileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final ExpoContentConverter converter;

    // ---------- 이미지 ----------

    /**
     * 이미지를 붙인다.
     *
     * <p>대표 이미지를 새로 지정하면 <b>기존 대표는 지우지 않고 상세로 내린다.</b> 지우면 주최사가 올린 사진이 말없이 사라진다.
     */
    @Transactional
    public ExpoImageResponse attachImage(
            Long expoId, Long clientUserId, AttachExpoImageRequest request) {
        requireHost(expoId, clientUserId);
        FileMetadata file = publicFileOrThrow(request.fileId());
        requireImage(file);
        if (expoImageRepository.existsByExpoIdAndFileId(expoId, request.fileId())) {
            throw new BusinessException(ErrorCode.EXPO_CONTENT_DUPLICATE_FILE);
        }
        if (request.imageType() == ExpoImageType.THUMBNAIL) {
            demoteExistingThumbnails(expoId);
        }

        ExpoImage saved =
                expoImageRepository.save(
                        ExpoImage.attach(
                                expoId,
                                request.fileId(),
                                request.imageType(),
                                request.altText(),
                                nextSortOrder(expoImageRepository.findMaxSortOrder(expoId))));
        return converter.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpoImageResponse> listImages(Long expoId) {
        return expoImageRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId).stream()
                .map(converter::toResponse)
                .toList();
    }

    @Transactional
    public void detachImage(Long expoId, Long imageId, Long clientUserId) {
        requireHost(expoId, clientUserId);
        ExpoImage image =
                expoImageRepository
                        .findById(imageId)
                        .filter(found -> found.belongsTo(expoId))
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_CONTENT_NOT_FOUND));
        expoImageRepository.delete(image);
    }

    // ---------- 자료 ----------

    @Transactional
    public ExpoFileResponse attachFile(
            Long expoId, Long clientUserId, AttachExpoFileRequest request) {
        requireHost(expoId, clientUserId);
        FileMetadata file = publicFileOrThrow(request.fileId());
        if (expoFileRepository.existsByExpoIdAndFileId(expoId, request.fileId())) {
            throw new BusinessException(ErrorCode.EXPO_CONTENT_DUPLICATE_FILE);
        }

        ExpoFile saved =
                expoFileRepository.save(
                        ExpoFile.attach(
                                expoId,
                                request.fileId(),
                                request.filePurpose(),
                                request.title(),
                                nextSortOrder(expoFileRepository.findMaxSortOrder(expoId))));
        return converter.toResponse(saved, file.getOriginalFilename());
    }

    /** 제목이 비어 있는 자료는 원본 파일명으로 보여 준다. 이름을 한 번에 모아 읽어 N+1 을 피한다. */
    @Transactional(readOnly = true)
    public List<ExpoFileResponse> listFiles(Long expoId) {
        List<ExpoFile> files = expoFileRepository.findByExpoIdOrderBySortOrderAscIdAsc(expoId);
        if (files.isEmpty()) {
            return List.of();
        }

        Map<Long, String> filenames =
                fileMetadataRepository
                        .findAllById(files.stream().map(ExpoFile::getFileId).distinct().toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        FileMetadata::getId,
                                        FileMetadata::getOriginalFilename,
                                        (first, second) -> first));

        Function<ExpoFile, ExpoFileResponse> map =
                file -> converter.toResponse(file, filenames.get(file.getFileId()));
        return files.stream().map(map).toList();
    }

    @Transactional
    public void detachFile(Long expoId, Long expoFileId, Long clientUserId) {
        requireHost(expoId, clientUserId);
        ExpoFile file =
                expoFileRepository
                        .findById(expoFileId)
                        .filter(found -> found.belongsTo(expoId))
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_CONTENT_NOT_FOUND));
        expoFileRepository.delete(file);
    }

    // ---------- 내부 ----------

    private void requireHost(Long expoId, Long clientUserId) {
        Expo expo =
                expoRepository
                        .findById(expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_NOT_FOUND));
        if (!expo.getHostClientId().equals(clientUserId)) {
            throw new BusinessException(ErrorCode.NOT_EXPO_HOST);
        }
    }

    private FileMetadata publicFileOrThrow(Long fileId) {
        FileMetadata file =
                fileMetadataRepository
                        .findById(fileId)
                        .filter(FileMetadata::isDownloadable)
                        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        if (!file.isPublic()) {
            throw new BusinessException(ErrorCode.EXPO_CONTENT_FILE_NOT_PUBLIC);
        }
        return file;
    }

    /**
     * 이미지 자리에 PDF 가 들어오는 것을 막는다.
     *
     * <p>업로드 용도({@code FilePurpose})가 이미 한 번 걸렀지만, 같은 공개 등급인 {@code EXPO_DOCUMENT} 로 올린 PDF 를 이미지로
     * 붙이는 길이 남아 있다.
     */
    private static void requireImage(FileMetadata file) {
        if (!file.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.EXPO_CONTENT_NOT_AN_IMAGE);
        }
    }

    private void demoteExistingThumbnails(Long expoId) {
        expoImageRepository
                .findByExpoIdAndImageType(expoId, ExpoImageType.THUMBNAIL)
                .forEach(ExpoImage::demoteToDetail);
    }

    private static int nextSortOrder(Integer currentMax) {
        return currentMax == null ? 0 : currentMax + 1;
    }
}
