package com.expo.booth.repository;

import com.expo.booth.entity.BoothContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 콘텐츠 영속성 접근 인터페이스. */
public interface BoothContentRepository extends JpaRepository<BoothContent, Long> {

    Optional<BoothContent> findByBoothAllocationId(Long boothAllocationId);

    Optional<BoothContent> findByIdAndClientUserId(Long id, Long clientUserId);

    Optional<BoothContent> findByBoothAllocationIdAndClientUserId(
            Long boothAllocationId, Long clientUserId);
}
