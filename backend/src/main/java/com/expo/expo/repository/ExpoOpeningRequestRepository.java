package com.expo.expo.repository;

import com.expo.expo.entity.ExpoEnums;
import com.expo.expo.entity.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.entity.ExpoOpeningRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.expo.expo.entity.ExpoOpeningRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoOpeningRequestRepository extends JpaRepository<ExpoOpeningRequest, Long> {
    Page<ExpoOpeningRequest> findByHostClientId(Long hostClientId, Pageable pageable);

    Page<ExpoOpeningRequest> findByStatusOrderBySubmittedAtAsc(
            OpeningRequestStatus status, Pageable pageable);
    /** 주최사 본인의 신청 목록. 최근 것부터. */
    List<ExpoOpeningRequest> findByHostClientIdOrderByIdDesc(Long hostClientId);

    /**
     * 관리자 심사 목록. 상태를 안 주면 전체.
     *
     * <p>{@code idx_expo_opening_requests_status (status, submitted_at)} 가 이미 있어 상태 필터가 인덱스를 탄다.
     */
    List<ExpoOpeningRequest> findByStatusOrderByIdDesc(OpeningRequestStatus status);

    List<ExpoOpeningRequest> findAllByOrderByIdDesc();
}
