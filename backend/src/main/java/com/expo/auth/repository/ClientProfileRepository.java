package com.expo.auth.repository;

import com.expo.auth.entity.ClientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link ClientProfile} 리포지토리.
 *
 * <p>사업자등록번호 사용 가능 여부 확인(A-API-005)과 클라이언트 회원가입(A-API-002)에서 공통으로 재사용한다.
 */
public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {

    /**
     * 정규화된 숫자 10자리 사업자등록번호가 이미 등록되어 있는지 확인한다.
     *
     * <p>입력은 반드시 하이픈이 제거된 10자리 숫자 문자열이어야 한다. 정규화는 {@link
     * com.expo.auth.service.BusinessNumberValidationService#normalize(String)} 에서 수행한다.
     */
    boolean existsByBusinessNumber(String businessNumber);
}
