package com.expo.file.controller;

import com.expo.auth.Role;
import com.expo.common.response.ApiResponse;
import com.expo.file.converter.FileConverter;
import com.expo.file.dto.FileMetadataResponse;
import com.expo.file.entity.FileMetadata;
import com.expo.file.entity.FilePurpose;
import com.expo.file.service.FileDownload;
import com.expo.file.service.FileService;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드·내려받기 (이슈 #93).
 *
 * <p>업로드·삭제는 로그인이 필요하고 <b>내려받기는 아니다</b> — 공개 파일은 인증 없이 나가야 한다. 프론트 인증이 {@code Authorization}
 * 헤더라 {@code <img src>} 가 토큰을 실을 수 없기 때문이다. 비공개 파일의 권한은 {@link FileService} 가 판정한다.
 *
 * <p>브라우저가 저장소로 직접 붙지 않고 여기서 바이트를 중계한다. 이유는 {@code docs/s3-presigned-url.md} 에 있다.
 */
@Tag(name = "File", description = "파일 업로드·내려받기")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    /** 공개 파일의 브라우저 캐시 수명(초). 키에 UUID 가 들어가 같은 URL 의 내용이 바뀌지 않는다. */
    private static final long PUBLIC_CACHE_SECONDS = 86400L;

    private final FileService fileService;
    private final FileConverter fileConverter;

    @Operation(summary = "파일 업로드", description = "용도(purpose)가 허용 형식·최대 크기·공개 범위를 결정한다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadataResponse>> upload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam FilePurpose purpose) {
        FileMetadata saved = fileService.upload(principal.getMemberId(), purpose, file);
        return ResponseEntity.ok(ApiResponse.ok(fileConverter.toResponse(saved)));
    }

    @Operation(summary = "파일 메타데이터 조회")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getMetadata(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long fileId) {
        FileMetadata metadata = fileService.get(fileId, userIdOf(principal), isAdmin(principal));
        return ResponseEntity.ok(ApiResponse.ok(fileConverter.toResponse(metadata)));
    }

    /**
     * 내용을 내려받는다.
     *
     * <p>이 응답만 {@link ApiResponse} 로 감싸지 않는다. 봉투에 넣으려면 바이트를 base64 로 부풀려야 하고, 무엇보다 {@code <img
     * src>} 가 JSON 을 그리지 못한다.
     */
    @Operation(summary = "파일 내려받기", description = "공개 파일은 인증 없이, 비공개 파일은 업로더 본인과 관리자만.")
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long fileId) {
        FileDownload download =
                fileService.download(fileId, userIdOf(principal), isAdmin(principal));
        FileMetadata metadata = download.metadata();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .contentLength(metadata.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(metadata))
                .cacheControl(cacheControl(metadata))
                .body(toResource(download.content()));
    }

    @Operation(summary = "파일 삭제", description = "논리 삭제다. 저장소의 객체는 남는다.")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long fileId) {
        fileService.delete(fileId, principal.getMemberId(), isAdmin(principal));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---------- 내부 ----------

    private static Resource toResource(InputStream content) {
        return new InputStreamResource(content);
    }

    /** 비로그인 요청은 principal 이 없다. 내려받기 경로가 인증을 요구하지 않으므로 정상적인 상황이다. */
    private static Long userIdOf(AuthPrincipal principal) {
        return principal == null ? null : principal.getMemberId();
    }

    private static boolean isAdmin(AuthPrincipal principal) {
        return principal != null && principal.getRole() == Role.ADMIN;
    }

    /**
     * 이미지는 화면에 그려야 하므로 {@code inline}, 나머지는 {@code attachment} 다.
     *
     * <p>파일명은 {@code filename*=UTF-8''...} 로만 준다. 한글 이름이 흔한데 {@code filename=} 은 ASCII 만 담을 수 있어
     * 브라우저마다 다르게 깨진다.
     */
    private static String contentDisposition(FileMetadata metadata) {
        String type = metadata.getContentType().startsWith("image/") ? "inline" : "attachment";
        String encoded =
                URLEncoder.encode(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                        .replace("+", "%20");
        return type + "; filename*=UTF-8''" + encoded;
    }

    /** 비공개 파일은 공용 프록시나 CDN 에 남으면 안 되므로 캐시를 막는다. */
    private static CacheControl cacheControl(FileMetadata metadata) {
        if (!metadata.isPublic()) {
            return CacheControl.noStore();
        }
        return CacheControl.maxAge(java.time.Duration.ofSeconds(PUBLIC_CACHE_SECONDS))
                .cachePublic();
    }
}
