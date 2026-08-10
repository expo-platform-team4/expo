package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNoticeRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 주최 클라이언트의 모집공고 생성 요청 영속성 접근 인터페이스. */
public interface RecruitmentNoticeRequestRepository
        extends JpaRepository<RecruitmentNoticeRequest, Long> {

    List<RecruitmentNoticeRequest> findAllByHostClientId(Long hostClientId);

    Optional<RecruitmentNoticeRequest> findByIdAndHostClientId(Long id, Long hostClientId);

    /** 장소 충돌 판정 시 동시 결정으로 인한 덮어쓰기를 막기 위해 행 잠금을 걸고 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecruitmentNoticeRequest r WHERE r.id = :id")
    Optional<RecruitmentNoticeRequest> findByIdForUpdate(@Param("id") Long id);
}
