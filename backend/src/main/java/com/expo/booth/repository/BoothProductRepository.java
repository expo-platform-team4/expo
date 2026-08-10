package com.expo.booth.repository;

import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothSalesStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 특정 모집공고에서 실제 판매되는 부스 상품 영속성 접근 인터페이스. */
public interface BoothProductRepository extends JpaRepository<BoothProduct, Long> {

    boolean existsByIdAndRecruitmentNoticeId(Long id, Long recruitmentNoticeId);

    boolean existsByRecruitmentNoticeIdAndBoothId(Long recruitmentNoticeId, Long boothId);

    List<BoothProduct> findAllByRecruitmentNoticeId(Long recruitmentNoticeId);

    List<BoothProduct> findAllByRecruitmentNoticeIdAndSalesStatus(
            Long recruitmentNoticeId, BoothSalesStatus salesStatus);
}
