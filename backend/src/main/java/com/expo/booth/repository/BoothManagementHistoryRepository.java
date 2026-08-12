package com.expo.booth.repository;

import com.expo.booth.entity.BoothManagementHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 배정·콘텐츠 운영 변경 이력 영속성 접근 인터페이스. */
public interface BoothManagementHistoryRepository
        extends JpaRepository<BoothManagementHistory, Long> {

    List<BoothManagementHistory> findAllByBoothContentIdOrderByCreatedAtDescIdDesc(
            Long boothContentId);
}
