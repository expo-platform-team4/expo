package com.expo.admin.repository;

import com.expo.admin.dto.AdminDashboardCountsResponse;
import com.expo.admin.dto.AdminPendingReviewResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 관리자 대시보드 조회용 MyBatis 매퍼.
 *
 * <p>{@code v_admin_dashboard_counts}, {@code v_admin_pending_reviews} 뷰를 그대로 읽는다. JPA 엔티티를 두지 않는
 * 이유는 물리 테이블이 아니라 여러 도메인 테이블을 합친 읽기 전용 뷰이기 때문이다.
 */
@Mapper
public interface AdminDashboardMapper {

    /** 관리자 대시보드 처리 대기 건수를 집계한 단일 행을 반환한다. */
    AdminDashboardCountsResponse findDashboardCounts();

    /** 심사 대기 목록(박람회 개최·배너·모집공고 생성 요청)을 통합 조회한다. */
    List<AdminPendingReviewResponse> findPendingReviews();
}
