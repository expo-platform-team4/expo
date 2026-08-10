package com.expo.participation.repository;

import com.expo.participation.entity.ApplicationOperationHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 참여 신청 운영 확인·보완 요청 이력 영속성 접근 인터페이스. */
public interface ApplicationOperationHistoryRepository
        extends JpaRepository<ApplicationOperationHistory, Long> {

    List<ApplicationOperationHistory> findAllByApplicationIdOrderByCreatedAtDesc(
            Long applicationId);

    Optional<ApplicationOperationHistory> findFirstByApplicationIdOrderByCreatedAtDesc(
            Long applicationId);
}
