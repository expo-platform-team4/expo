package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentResultItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 모집 결과 항목 영속성 접근 인터페이스. */
public interface RecruitmentResultItemRepository
        extends JpaRepository<RecruitmentResultItem, Long> {

    List<RecruitmentResultItem> findAllByRecruitmentResultId(Long recruitmentResultId);

    List<RecruitmentResultItem> findAllByRecruitmentResultIdIn(
            Collection<Long> recruitmentResultIds);
}
