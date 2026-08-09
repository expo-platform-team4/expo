package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNotice;
import org.springframework.data.jpa.repository.JpaRepository;

/** 허용된 장소 요청을 기준으로 관리자가 작성·게시하는 기업 모집공고 영속성 접근 인터페이스. */
public interface RecruitmentNoticeRepository extends JpaRepository<RecruitmentNotice, Long> {}
