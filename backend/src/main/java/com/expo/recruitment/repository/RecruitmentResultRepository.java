package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 모집 결과 스냅샷 영속성 접근 인터페이스. */
public interface RecruitmentResultRepository extends JpaRepository<RecruitmentResult, Long> {

    boolean existsByRecruitmentNoticeId(Long recruitmentNoticeId);

    Optional<RecruitmentResult> findByRecruitmentNoticeId(Long recruitmentNoticeId);

    Optional<RecruitmentResult> findByIdAndHostClientId(Long id, Long hostClientId);
}
