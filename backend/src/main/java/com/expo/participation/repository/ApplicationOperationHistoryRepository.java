package com.expo.participation.repository;

import com.expo.participation.entity.ApplicationOperationActionType;
import com.expo.participation.entity.ApplicationOperationHistory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 참여 신청 운영 확인·보완 요청 이력 영속성 접근 인터페이스. */
public interface ApplicationOperationHistoryRepository
        extends JpaRepository<ApplicationOperationHistory, Long> {

    List<ApplicationOperationHistory> findAllByApplicationIdOrderByCreatedAtDescIdDesc(
            Long applicationId);

    /** 신청 기업에게 공개할 유형만 거른 이력. 관리자 메모(MEMO_UPDATED)는 내부용이라 뺀다. */
    List<ApplicationOperationHistory>
            findAllByApplicationIdAndActionTypeInOrderByCreatedAtDescIdDesc(
                    Long applicationId, Collection<ApplicationOperationActionType> actionTypes);

    /** {@code createdAt} 이 같은 이력이 있어도 {@code id} 로 최신 순서를 결정적으로 가린다. */
    Optional<ApplicationOperationHistory> findFirstByApplicationIdOrderByCreatedAtDescIdDesc(
            Long applicationId);

    /**
     * 주어진 유형들 중 가장 최근 이력만 가린다. 보완 요청·완료 사이에 운영 확인(CHECKED) 등 무관한 이력이
     * 끼어들어도 보완 요청/완료 흐름 판정에 영향을 주지 않게 하려고 쓴다.
     */
    Optional<ApplicationOperationHistory>
            findFirstByApplicationIdAndActionTypeInOrderByCreatedAtDescIdDesc(
                    Long applicationId, Collection<ApplicationOperationActionType> actionTypes);
}
