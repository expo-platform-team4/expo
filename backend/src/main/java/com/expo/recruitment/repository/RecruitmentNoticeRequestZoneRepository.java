package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNoticeRequestZone;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 모집공고 생성 요청이 고른 구역 목록 영속성 접근 인터페이스. */
public interface RecruitmentNoticeRequestZoneRepository
        extends JpaRepository<RecruitmentNoticeRequestZone, RecruitmentNoticeRequestZone.Id> {

    List<RecruitmentNoticeRequestZone> findAllByIdRequestId(Long requestId);

    @Query(
            "SELECT z.id.venueZoneId FROM RecruitmentNoticeRequestZone z "
                    + "WHERE z.id.requestId = :requestId")
    List<Long> findVenueZoneIdsByRequestId(@Param("requestId") Long requestId);

    List<RecruitmentNoticeRequestZone> findAllByIdRequestIdIn(Collection<Long> requestIds);
}
