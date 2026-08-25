package com.expo.banner.repository;

import com.expo.banner.entity.BannerApplication;
import com.expo.banner.entity.BannerApplication.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerApplicationRepository extends JpaRepository<BannerApplication, Long> {

    /**
     * B-API-020: 내 배너 신청 목록·상태 조회. 최근 신청이 먼저다.
     *
     * <p><b>정렬이 없으면 페이지를 넘길 때 같은 신청이 또 나온다.</b> LIMIT/OFFSET 은 정해진
     * 순서 위에서만 의미가 있고, 순서를 안 주면 매 쿼리마다 순서가 달라질 수 있어 어떤 행은
     * 두 번 보이고 어떤 행은 영영 안 보인다.
     */
    Page<BannerApplication> findByClientUserIdOrderByIdDesc(Long clientUserId, Pageable pageable);

    /**
     * B-API-021: 관리자 배너 신청 목록 조회 (심사 상태 필터 옵션, 신청 순).
     * reviewStatus 가 null 이면 전체 상태를 대상으로 한다.
     *
     * <p><b>{@code id} 로 동점을 깬다.</b> {@code createdAt} 만으로는 부족하다 — 같은 초에 들어온
     * 신청들은 순서가 정해지지 않아 <b>페이지를 넘길 때 같은 신청이 또 나온다.</b> 실제로 그랬다:
     * 2페이지에 1페이지의 신청이 다시 보였다.
     */
    @Query(
            """
            select a from BannerApplication a
            where (:reviewStatus is null or a.reviewStatus = :reviewStatus)
            order by a.createdAt asc, a.id asc
            """)
    Page<BannerApplication> searchForAdmin(
            @Param("reviewStatus") ReviewStatus reviewStatus, Pageable pageable);
}
