package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNoticeRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 모집공고 생성 요청 처리 이력 영속성 접근 인터페이스. */
public interface RecruitmentNoticeRequestHistoryRepository
        extends JpaRepository<RecruitmentNoticeRequestHistory, Long> {}
