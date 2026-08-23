package com.expo.member.service;

import com.expo.auth.entity.User;
import com.expo.auth.repository.UserRepository;
import com.expo.auth.service.NicknameAvailabilityService;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.file.entity.FileMetadata;
import com.expo.file.entity.FilePurpose;
import com.expo.file.service.FileService;
import com.expo.member.dto.MemberProfileResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일반 회원·클라이언트 공통 마이페이지 프로필 조회·닉네임 변경·프로필이미지 변경 (A-API-015, A-API-016, A-API-017). */
@Service
@Transactional(readOnly = true)
public class MemberProfileService {

    private final UserRepository userRepository;
    private final NicknameAvailabilityService nicknameAvailabilityService;
    private final FileService fileService;

    public MemberProfileService(
            UserRepository userRepository,
            NicknameAvailabilityService nicknameAvailabilityService,
            FileService fileService) {
        this.userRepository = userRepository;
        this.nicknameAvailabilityService = nicknameAvailabilityService;
        this.fileService = fileService;
    }

    /**
     * 내 프로필을 조회한다 (A-API-015).
     *
     * @throws BusinessException 사용자가 없으면 ({@code MEMBER_NOT_FOUND})
     */
    public MemberProfileResponse getMyProfile(Long userId) {
        User user = findUserOrThrow(userId);
        return toResponse(user);
    }

    /**
     * 닉네임을 변경한다 (A-API-016).
     *
     * <p>형식 검증·중복 검사는 {@link NicknameAvailabilityService}(A-API-004)와 동일한 규칙을 그대로 재사용한다.
     *
     * @throws com.expo.auth.exception.InvalidNicknameException 필수값·길이 오류
     * @throws BusinessException 닉네임 중복({@code DUPLICATE_NICKNAME}) 또는 사용자 없음({@code
     *     MEMBER_NOT_FOUND})
     */
    @Transactional
    public MemberProfileResponse changeNickname(Long userId, String rawNickname) {
        String normalized = nicknameAvailabilityService.normalize(rawNickname);
        User user = findUserOrThrow(userId);

        // 기존 닉네임 그대로 재요청한 경우는 중복 검사를 건너뛴다 (자기 자신과 비교하면 항상 걸리므로).
        if (!normalized.equals(user.getNickname()) && userRepository.existsByNickname(normalized)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.changeNickname(normalized);
        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * 프로필 이미지를 등록·교체한다 (A-API-017).
     *
     * <p>이미지 바이트는 이미 {@code POST /api/files}(purpose=PROFILE_IMAGE)로 올라가 있다고 가정하고,
     * 그 결과인 {@code fileId} 를 받아 내 계정에 연결하기만 한다. 파일이 정말 내가 올린 게 맞는지는 여기서
     * 한 번 더 확인한다 — {@link FileService#get} 은 PROFILE_IMAGE 가 공개(PUBLIC) 파일이라 아무 fileId
     * 나 넘겨도 존재만 하면 내주기 때문에, 다른 사람이 올린 공개 파일(예: 박람회 이미지)을 내 프로필에
     * 연결하는 걸 막으려면 소유자 검사를 따로 해야 한다.
     *
     * <p><b>{@code purpose} 는 다시 확인하지 못한다</b> — {@code file_metadata} 는 애초에 업로드 용도를
     * 저장하지 않는다({@link FilePurpose} 주석 참고, 11개 도메인이 테이블 하나를 공유해서다). 대신 저장돼
     * 있는 {@code contentType} 이 {@link FilePurpose#PROFILE_IMAGE} 가 허용하는 형식(JPEG/PNG/WEBP)인지는
     * 확인한다 — 이걸 빼먹으면 내가 올린 PDF(예: 정산 리포트)의 fileId 를 그대로 여기 보내도 통과해서,
     * 나중에 그 fileId 로 아바타를 그리려는 {@code <img>} 가 깨진다.
     *
     * <p>이전에 연결돼 있던 파일은 지우지 않는다 — 다른 화면이 그 fileId 를 아직 들고 있을 수 있어(예: 캐시된
     * 응답), 여기서 지우면 그쪽이 깨진다. 정리는 {@link FileService#delete} 의 정책과 같이 나중 배치의 몫으로
     * 남긴다.
     *
     * @throws BusinessException 파일이 없거나({@code FILE_NOT_FOUND}), 내가 올린 파일이 아니거나({@code
     *     FILE_NOT_FOUND}), 이미지 형식이 아니거나({@code FILE_CONTENT_TYPE_NOT_ALLOWED}), 사용자가 없으면
     *     ({@code MEMBER_NOT_FOUND})
     */
    @Transactional
    public MemberProfileResponse changeProfileImage(Long userId, Long fileId) {
        User user = findUserOrThrow(userId);

        FileMetadata file = fileService.get(fileId, userId, false);
        if (!file.isUploadedBy(userId)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!FilePurpose.PROFILE_IMAGE.allows(file.getContentType())) {
            throw new BusinessException(ErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED);
        }

        user.updateProfileImage(fileId, Instant.now());
        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * 프로필 이미지를 지운다 (A-API-017). 지운 뒤에는 기본 이미지로 보인다.
     *
     * <p>연결만 끊는다 — {@code FileMetadata} 자체는 지우지 않는다. {@link FileService#delete} 의 정책과
     * 같은 이유다(다른 화면이 같은 fileId 를 참조 중일 수 있고, 참조가 0인지 확인하는 정리는 나중 배치의
     * 몫이다).
     */
    @Transactional
    public MemberProfileResponse removeProfileImage(Long userId) {
        User user = findUserOrThrow(userId);
        user.clearProfileImage(Instant.now());
        userRepository.save(user);
        return toResponse(user);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberProfileResponse toResponse(User user) {
        return new MemberProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getPhoneNumber(),
                user.getProfileImageFileId(),
                user.getProfileImageUpdatedAt(),
                user.getCreatedAt());
    }
}
