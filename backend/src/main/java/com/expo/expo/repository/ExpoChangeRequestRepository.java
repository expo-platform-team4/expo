package com.expo.expo.repository;

import com.expo.expo.entity.ExpoAttachments.ExpoChangeRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoChangeRequestRepository extends JpaRepository<ExpoChangeRequest, Long> {
    List<ExpoChangeRequest> findByExpoIdOrderByCreatedAtDesc(Long expoId);
}
