package com.expo.admin.service;

import com.expo.admin.dto.AdminUserDetailResponse;
import com.expo.admin.dto.AdminUserSearchPage;
import com.expo.admin.repository.AdminUserMapper;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 회원·클라이언트 계정 조회 (E-API-012, E-API-013). */
@Service
@Transactional(readOnly = true)
public class AdminUserService {

    /** 한 번에 너무 많이 퍼가지 못하게 막는다. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminUserMapper adminUserMapper;

    public AdminUserService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    /**
     * 이메일·닉네임·회사명 부분 일치 검색, role·상태 필터를 지원한다 (E-API-012).
     *
     * <p>{@code offset} 을 {@code long} 으로 계산한다. {@code int} 로 곱하면 {@code page} 가 큰 값일 때
     * 오버플로해 음수 offset 이 되고, PostgreSQL 이 {@code OFFSET must not be negative} 로 거절해 500 이
     * 나간다.
     *
     * @param page 0부터. 음수는 0으로, 데이터 범위를 넘는 값은 빈 목록으로 돌아온다
     * @param size 최대 {@value #MAX_PAGE_SIZE}
     */
    public AdminUserSearchPage searchUsers(
            String keyword, String role, String accountStatus, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long offset = (long) safePage * safeSize;

        return new AdminUserSearchPage(
                adminUserMapper.countSearch(keyword, role, accountStatus),
                safePage,
                safeSize,
                adminUserMapper.search(keyword, role, accountStatus, safeSize, offset));
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
