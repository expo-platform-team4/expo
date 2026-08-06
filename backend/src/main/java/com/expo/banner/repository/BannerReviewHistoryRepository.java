package com.expo.banner.repository;

import com.expo.banner.entity.BannerReviewHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerReviewHistoryRepository extends JpaRepository<BannerReviewHistory, Long> {

    List<BannerReviewHistory> findByBannerApplicationIdOrderByReviewedAtAscIdAsc(
            Long bannerApplicationId);
}
