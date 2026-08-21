package com.expo.member.service;

import com.expo.auth.entity.User;
import com.expo.auth.repository.UserRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 상태에서의 비밀번호 변경. */
@Service
@Transactional(readOnly = true)
public class MemberPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberPasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 현재 비밀번호 확인 후 새 비밀번호로 교체한다.
     *
     * @throws BusinessException 회원이 없거나({@code MEMBER_NOT_FOUND}), 현재 비밀번호가 틀리거나({@code
     *     CURRENT_PASSWORD_MISMATCH}), 새 비밀번호 확인이 일치하지 않는 경우({@code PASSWORD_MISMATCH})
     */
    @Transactional
    public void changePassword(
            Long userId, String currentPassword, String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        User user = findUserOrThrow(userId);
        requireMatches(user, currentPassword);

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void requireMatches(User user, String rawPassword) {
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
    }
}
