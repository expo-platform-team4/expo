package com.expo.banner.repository;

import com.expo.banner.entity.BannerApplication;
import com.expo.banner.entity.BannerApplication.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerApplicationRepository extends JpaRepository<BannerApplication, Long> {

    /** B-API-020: 내 배너 신청 목록·상태 조회 */
    Page<BannerApplication> findByClientUserId(Long clientUserId, Pageable pageable);

    /**
     * B-API-021: 관리자 배너 신청 목록 조회 (심사 상태 필터 옵션, 신청 순).
     * reviewStatus 가 null 이면 전체 상태를 대상으로 한다.
     */
    @Query(
            """
            select a from BannerApplication a
            where (:reviewStatus is null or a.reviewStatus = :reviewStatus)
            order by a.createdAt asc
            """)
    Page<BannerApplication> searchForAdmin(
            @Param("reviewStatus") ReviewStatus reviewStatus, Pageable pageable);
}
