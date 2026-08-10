package com.expo.participation.repository;

import com.expo.participation.entity.ParticipationApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 참여 기업의 신청서 영속성 접근 인터페이스. */
public interface ParticipationApplicationRepository
        extends JpaRepository<ParticipationApplication, Long> {

    Optional<ParticipationApplication> findByIdAndClientUserId(Long id, Long clientUserId);

    List<ParticipationApplication> findAllByRecruitmentNoticeId(Long recruitmentNoticeId);
}
