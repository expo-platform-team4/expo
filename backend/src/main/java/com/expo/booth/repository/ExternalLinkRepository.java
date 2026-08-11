package com.expo.booth.repository;

import com.expo.booth.entity.ExternalLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 콘텐츠 외부 링크 영속성 접근 인터페이스. */
public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {

    List<ExternalLink> findAllByBoothContentIdOrderBySortOrderAscIdAsc(Long boothContentId);

    Optional<ExternalLink> findByIdAndBoothContentId(Long id, Long boothContentId);
}
