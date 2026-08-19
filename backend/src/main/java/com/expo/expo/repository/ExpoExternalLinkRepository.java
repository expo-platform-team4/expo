package com.expo.expo.repository;

import com.expo.expo.entity.ExpoAttachments.ExternalLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoExternalLinkRepository extends JpaRepository<ExternalLink, Long> {
    List<ExternalLink> findByExpoIdOrderBySortOrderAscIdAsc(Long expoId);
}
