package com.expo.expo.repository;

import com.expo.expo.entity.ExpoCategory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 박람회 ↔ 카테고리 N:M 연결 영속성 접근 인터페이스. */
public interface ExpoCategoryRepository extends JpaRepository<ExpoCategory, ExpoCategory.Id> {}
