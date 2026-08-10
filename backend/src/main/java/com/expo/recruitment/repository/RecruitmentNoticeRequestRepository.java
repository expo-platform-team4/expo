package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/** 주최 클라이언트의 모집공고 생성 요청 영속성 접근 인터페이스. */
public interface RecruitmentNoticeRequestRepository
        extends JpaRepository<RecruitmentNoticeRequest, Long> {}
