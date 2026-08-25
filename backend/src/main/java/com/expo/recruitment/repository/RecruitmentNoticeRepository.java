package com.expo.recruitment.repository;

import com.expo.recruitment.entity.RecruitmentNotice;
import com.expo.recruitment.entity.RecruitmentNoticeStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 허용된 장소 요청을 기준으로 관리자가 작성·게시하는 기업 모집공고 영속성 접근 인터페이스. */
public interface RecruitmentNoticeRepository extends JpaRepository<RecruitmentNotice, Long> {

    boolean existsByRequestId(Long requestId);

    /** 박람회 하나에 딸린 모집공고 목록. 참여기업 조회처럼 expo 기준으로 거슬러 올라갈 때 쓴다. */
    List<RecruitmentNotice> findAllByExpoId(Long expoId);

    List<RecruitmentNotice> findAllByStatus(RecruitmentNoticeStatus status);

    /** 이미 공고가 만들어진 요청 ID 들. 요청 목록에서 "초안 생성 가능" 판단에 쓴다. */
    @Query("select n.requestId from RecruitmentNotice n where n.requestId in :requestIds")
    List<Long> findRequestIdsByRequestIdIn(@Param("requestIds") Collection<Long> requestIds);

    Optional<RecruitmentNotice> findByIdAndStatus(Long id, RecruitmentNoticeStatus status);

    boolean existsByIdInAndStatusNot(Collection<Long> ids, RecruitmentNoticeStatus status);

    /** 신청 시작일이 지났는데 아직 SCHEDULED 인 공고. OPEN 자동 전환 대상 조회용. */
    List<RecruitmentNotice> findAllByStatusAndApplicationStartAtBefore(
            RecruitmentNoticeStatus status, Instant applicationStartAt);

    /** 신청 종료일이 지났는데 아직 OPEN 인 공고. 자동 마감 대상 조회용. */
    List<RecruitmentNotice> findAllByStatusAndApplicationEndAtBefore(
            RecruitmentNoticeStatus status, Instant applicationEndAt);
}
