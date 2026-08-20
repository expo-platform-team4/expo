package com.expo.member.service;

import com.expo.auth.entity.AccountStatus;
import com.expo.auth.entity.User;
import com.expo.auth.repository.UserRepository;
import com.expo.auth.service.LoginService;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 (A-API-018). 소프트 삭제 — {@code users} 행을 지우지 않고 {@code account_status} 만 바꾼다.
 *
 * <p>주문·티켓 등 다른 도메인의 데이터는 그대로 둔다. 정산·환불 이력 등에서 회원을 계속 참조해야 해서, 탈퇴했다고
 * 지워 버리면 그쪽이 깨진다 — 탈퇴 여부는 {@code users.account_status} 하나로만 판단한다.
 *
 * <p>탈퇴 처리 뒤에는 {@link LoginService#logout}으로 이 회원의 모든 Refresh Token 을 폐기한다. 다만
 * <b>이미 발급된 Access Token(JWT)은 만료 전까지 계속 유효하다</b> — Stateless 인증이라 매 요청마다 탈퇴 여부를
 * 다시 조회하지 않는 한 막을 방법이 없다. 짧은 Access Token 수명에 기대는 것으로 충분하다고 보고 지금은
 * 손대지 않는다.
 */
@Service
public class MemberWithdrawalService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginService loginService;

    public MemberWithdrawalService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginService loginService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginService = loginService;
    }

    /**
     * 본인 확인(현재 비밀번호) 후 탈퇴 처리한다.
     *
     * @throws BusinessException 회원이 없거나({@code MEMBER_NOT_FOUND}), 비밀번호가 틀리거나({@code
     *     MEMBER_WITHDRAWAL_PASSWORD_MISMATCH}), 이미 탈퇴한 계정인 경우({@code WITHDRAWN_ACCOUNT})
     */
    @Transactional
    public void withdraw(Long userId, String rawPassword) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.WITHDRAWN_ACCOUNT);
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWAL_PASSWORD_MISMATCH);
        }

        user.withdraw(Instant.now());
        userRepository.save(user);

        // 탈퇴 후에도 남아 있는 Refresh Token 으로 재로그인·재발급이 되면 안 된다.
        loginService.logout(userId);
    }
}
