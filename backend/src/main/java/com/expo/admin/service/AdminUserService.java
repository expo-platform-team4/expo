package com.expo.admin.service;

import com.expo.admin.dto.AdminUserDetailResponse;
import com.expo.admin.dto.AdminUserSummaryResponse;
import com.expo.admin.repository.AdminUserMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 회원·클라이언트 계정 조회 (E-API-012, E-API-013). */
@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;

    public AdminUserService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    /** 이메일·닉네임·회사명 부분 일치 검색, role·상태 필터를 지원한다 (E-API-012). */
    public List<AdminUserSummaryResponse> searchUsers(
            String keyword, String role, String accountStatus) {
        return adminUserMapper.search(keyword, role, accountStatus);
    }

    /**
     * @throws BusinessException 계정이 없으면 {@code ADMIN_USER_NOT_FOUND}
     */
    public AdminUserDetailResponse getUser(Long userId) {
        return adminUserMapper
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));
    }
}
