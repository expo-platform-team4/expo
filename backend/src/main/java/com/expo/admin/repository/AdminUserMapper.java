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
     * @param limit 한 페이지 크기
     * @param offset 건너뛸 개수. {@code page * size} 가 {@code int} 를 넘길 수 있어 {@code long} 이다
     */
    List<AdminUserSummaryResponse> search(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("accountStatus") String accountStatus,
            @Param("limit") int limit,
            @Param("offset") long offset);

    /** {@link #search} 와 동일한 필터로 전체 건수를 센다. 페이지 계산용. */
    long countSearch(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("accountStatus") String accountStatus);

    Optional<AdminUserDetailResponse> findById(@Param("userId") Long userId);
}
