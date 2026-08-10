package com.expo.expo.repository;

import com.expo.expo.domain.ExpoAttachments.ExpoChangeRequest;
import com.expo.expo.domain.ExpoAttachments.ExpoFile;
import com.expo.expo.domain.ExpoAttachments.ExpoImage;
import com.expo.expo.domain.ExpoAttachments.ExternalLink;
import com.expo.expo.domain.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.domain.ExpoOpeningRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * V1 스키마 기준 부속 Repository 모음.
 */
public final class ExpoRepositories {

    private ExpoRepositories() {}

    public interface ExpoOpeningRequestRepository extends JpaRepository<ExpoOpeningRequest, Long> {

        Page<ExpoOpeningRequest> findByHostClientId(Long hostClientId, Pageable pageable);

        /** 관리자 심사 대기열 — idx_expo_opening_requests_status(status, submitted_at) */
        Page<ExpoOpeningRequest> findByStatusOrderBySubmittedAtAsc(
                OpeningRequestStatus status, Pageable pageable);
    }

    public interface ExpoImageRepository extends JpaRepository<ExpoImage, Long> {
        List<ExpoImage> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);
    }

    public interface ExpoFileRepository extends JpaRepository<ExpoFile, Long> {
        List<ExpoFile> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);
    }

    public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {
        List<ExternalLink> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);
    }

    public interface ExpoChangeRequestRepository extends JpaRepository<ExpoChangeRequest, Long> {
        List<ExpoChangeRequest> findByExpoIdOrderByCreatedAtDesc(Long expoId);
    }
}
