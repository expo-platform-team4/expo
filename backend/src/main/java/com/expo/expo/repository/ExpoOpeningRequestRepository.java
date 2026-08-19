package com.expo.expo.repository;

import com.expo.expo.entity.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.entity.ExpoOpeningRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoOpeningRequestRepository extends JpaRepository<ExpoOpeningRequest, Long> {
    Page<ExpoOpeningRequest> findByHostClientId(Long hostClientId, Pageable pageable);

    Page<ExpoOpeningRequest> findByStatusOrderBySubmittedAtAsc(
            OpeningRequestStatus status, Pageable pageable);
}
