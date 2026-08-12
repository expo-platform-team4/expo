package com.expo.booth.repository;

import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 특정 모집공고에서 실제 판매되는 부스 상품 영속성 접근 인터페이스. */
public interface BoothProductRepository extends JpaRepository<BoothProduct, Long> {

    boolean existsByIdAndRecruitmentNoticeId(Long id, Long recruitmentNoticeId);

    boolean existsByRecruitmentNoticeIdAndBoothId(Long recruitmentNoticeId, Long boothId);

    /**
     * 같은 부스를 상품으로 등록한 다른 공고 ID 목록. 상품 자체가 취소되지 않은 것만 대상으로 한다.
     *
     * <p>공고 자체가 취소됐는지는 여기서 판단하지 않는다 - 호출 쪽에서 {@code RecruitmentNoticeRepository} 로
     * 확인해야 "취소된 공고의 상품"과 "살아있는 공고의 상품"을 구분할 수 있다.
     */
    @Query(
            "SELECT DISTINCT bp.recruitmentNoticeId FROM BoothProduct bp "
                    + "WHERE bp.boothId = :boothId AND bp.recruitmentNoticeId <> :recruitmentNoticeId "
                    + "AND bp.salesStatus <> :excludedStatus")
    List<Long> findOtherRecruitmentNoticeIdsUsingBooth(
            @Param("boothId") Long boothId,
            @Param("recruitmentNoticeId") Long recruitmentNoticeId,
            @Param("excludedStatus") BoothSalesStatus excludedStatus);

    List<BoothProduct> findAllByRecruitmentNoticeId(Long recruitmentNoticeId);

    List<BoothProduct> findAllByRecruitmentNoticeIdAndSalesStatus(
            Long recruitmentNoticeId, BoothSalesStatus salesStatus);
}
