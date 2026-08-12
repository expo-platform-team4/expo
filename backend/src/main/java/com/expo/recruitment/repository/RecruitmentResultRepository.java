package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentResult;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 모집 결과 스냅샷 영속성 접근 인터페이스. */
public interface RecruitmentResultRepository extends JpaRepository<RecruitmentResult, Long> {

    boolean existsByRecruitmentNoticeId(Long recruitmentNoticeId);

    Optional<RecruitmentResult> findByRecruitmentNoticeId(Long recruitmentNoticeId);

    Optional<RecruitmentResult> findByIdAndHostClientId(Long id, Long hostClientId);

    /** 전달·취소처럼 읽고 그 결과로 분기하는 관리자 처리 앞에서 행 잠금을 걸어 동시 처리를 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecruitmentResult r WHERE r.id = :id")
    Optional<RecruitmentResult> findByIdForUpdate(@Param("id") Long id);

    /** 주최자 확인처럼 읽고 그 결과로 분기하는 처리 앞에서 행 잠금을 걸어 동시 처리를 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecruitmentResult r WHERE r.id = :id AND r.hostClientId = :hostClientId")
    Optional<RecruitmentResult> findByIdAndHostClientIdForUpdate(
            @Param("id") Long id, @Param("hostClientId") Long hostClientId);
}
