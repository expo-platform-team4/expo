package com.expo.booth.repository;

import com.expo.booth.entity.BoothTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

/** 재사용 가능한 부스 형태 템플릿 영속성 접근 인터페이스. */
public interface BoothTemplateRepository extends JpaRepository<BoothTemplate, Long> {

    boolean existsByShapeCode(String shapeCode);
}
