package com.expo.member.service;

import com.expo.auth.entity.User;
import com.expo.auth.repository.UserRepository;
import com.expo.auth.service.NicknameAvailabilityService;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.member.dto.MemberProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일반 회원·클라이언트 공통 마이페이지 프로필 조회·닉네임 변경 (A-API-015, A-API-016). */
@Service
@Transactional(readOnly = true)
public class MemberProfileService {

    private final UserRepository userRepository;
    private final NicknameAvailabilityService nicknameAvailabilityService;

    public MemberProfileService(
            UserRepository userRepository,
            NicknameAvailabilityService nicknameAvailabilityService) {
        this.userRepository = userRepository;
        this.nicknameAvailabilityService = nicknameAvailabilityService;
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
