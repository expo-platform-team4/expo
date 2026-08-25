package com.expo.expo.repository;

import com.expo.expo.entity.ExpoOpeningRequestCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 박람회 개최 신청이 고른 카테고리 목록 영속성 접근 인터페이스. */
public interface ExpoOpeningRequestCategoryRepository
        extends JpaRepository<ExpoOpeningRequestCategory, ExpoOpeningRequestCategory.Id> {

    @Query(
            "SELECT c.id.categoryId FROM ExpoOpeningRequestCategory c "
                    + "WHERE c.id.openingRequestId = :requestId")
    List<Long> findCategoryIdsByOpeningRequestId(@Param("requestId") Long requestId);

    List<ExpoOpeningRequestCategory> findAllByIdOpeningRequestIdIn(Collection<Long> requestIds);

    @Modifying
    @Query("DELETE FROM ExpoOpeningRequestCategory c WHERE c.id.openingRequestId = :requestId")
    void deleteAllByOpeningRequestId(@Param("requestId") Long requestId);
}
