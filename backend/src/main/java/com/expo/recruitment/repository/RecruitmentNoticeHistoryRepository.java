package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNoticeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 모집공고 운영 변경 이력 영속성 접근 인터페이스. */
public interface RecruitmentNoticeHistoryRepository
        extends JpaRepository<RecruitmentNoticeHistory, Long> {}
