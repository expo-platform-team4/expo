package com.expo.participation.repository;

import com.expo.participation.entity.ParticipationApplication;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 참여 기업의 신청서 영속성 접근 인터페이스. */
public interface ParticipationApplicationRepository
        extends JpaRepository<ParticipationApplication, Long> {

    Optional<ParticipationApplication> findByIdAndClientUserId(Long id, Long clientUserId);

    List<ParticipationApplication> findAllByRecruitmentNoticeId(Long recruitmentNoticeId);

    /** 운영 이력을 읽고 그 결과로 분기하는 처리(보완 완료 등) 앞에서 행 잠금을 걸어 동시 처리를 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ParticipationApplication a WHERE a.id = :id")
    Optional<ParticipationApplication> findByIdForUpdate(@Param("id") Long id);
}
