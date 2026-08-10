package com.expo.admin.service;

import com.expo.admin.dto.AdminDashboardCountsResponse;
import com.expo.admin.dto.AdminPendingReviewResponse;
import com.expo.admin.repository.AdminDashboardMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 대시보드 핵심 현황·대기 업무 조회 (E-API-010, E-API-011). */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminDashboardMapper adminDashboardMapper;

    public AdminDashboardService(AdminDashboardMapper adminDashboardMapper) {
        this.adminDashboardMapper = adminDashboardMapper;
    }

    /** 관리자 대시보드 핵심 현황(처리 대기 건수 집계)을 조회한다. */
    public AdminDashboardCountsResponse getDashboardSummary() {
        return adminDashboardMapper.findDashboardCounts();
    }

    /** 개최·배너·모집공고 생성 요청 등 대기 업무 목록을 통합 조회한다. */
    public List<AdminPendingReviewResponse> getPendingTasks() {
        return adminDashboardMapper.findPendingReviews();
    }
}
