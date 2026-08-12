package com.expo.booth.repository;

import com.expo.booth.entity.ExternalLink;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 콘텐츠 외부 링크 영속성 접근 인터페이스. */
public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {

    List<ExternalLink> findAllByBoothContentIdOrderBySortOrderAscIdAsc(Long boothContentId);

    /** 목록 조회에서 콘텐츠별로 매번 조회하지 않도록 페이지에 담긴 콘텐츠 ID를 한 번에 모아 조회한다. */
    List<ExternalLink> findAllByBoothContentIdInOrderBySortOrderAscIdAsc(
            Collection<Long> boothContentIds);

    Optional<ExternalLink> findByIdAndBoothContentId(Long id, Long boothContentId);
}
