package com.expo.admin.repository;

import com.expo.admin.dto.AdminUserDetailResponse;
import com.expo.admin.dto.AdminUserSummaryResponse;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {

    /**
     * @param keyword 이메일·닉네임·회사명 부분 일치 검색어 (없으면 전체)
     * @param role 역할 필터 (없으면 전체)
     * @param accountStatus 계정 상태 필터 (없으면 전체)
     */
    List<AdminUserSummaryResponse> search(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("accountStatus") String accountStatus);

    Optional<AdminUserDetailResponse> findById(@Param("userId") Long userId);
}
