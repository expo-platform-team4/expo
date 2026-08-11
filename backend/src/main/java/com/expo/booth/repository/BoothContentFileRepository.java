package com.expo.booth.repository;

import com.expo.booth.entity.BoothContentFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 부스 콘텐츠 첨부 파일 영속성 접근 인터페이스. */
public interface BoothContentFileRepository extends JpaRepository<BoothContentFile, Long> {

    List<BoothContentFile> findAllByBoothContentIdOrderBySortOrderAscIdAsc(Long boothContentId);

    Optional<BoothContentFile> findByIdAndBoothContentId(Long id, Long boothContentId);
}
