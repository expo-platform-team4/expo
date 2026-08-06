package com.expo.expo.repository;

import com.expo.expo.entity.ExpoReviewHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoReviewHistoryRepository extends JpaRepository<ExpoReviewHistory, Long> {

    List<ExpoReviewHistory> findByExpoIdOrderByReviewedAtAsc(Long expoId);
}
