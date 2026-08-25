package com.expo.auth.repository;

import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@link PhoneVerification} 리포지토리. */
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
      UPDATE PhoneVerification p
      SET p.status = :expiredStatus
      WHERE p.phoneNumber = :phoneNumber
        AND p.status = :requestedStatus
      """)
    int expireRequestedByPhoneNumber(
            @Param("phoneNumber") String phoneNumber,
            @Param("requestedStatus") PhoneVerificationStatus requestedStatus,
            @Param("expiredStatus") PhoneVerificationStatus expiredStatus);

    /**
     * 이 번호로 마지막에 인증번호를 요청한 시각. 한 건도 없으면 {@code null}.
     *
     * <p>재요청 간격 제한에 쓴다. 상태를 보지 않는 것이 중요하다 — 직전 건은 재요청 때
     * {@code EXPIRED} 로 바뀌므로, {@code REQUESTED} 만 세면 두 번째 요청부터 제한이 풀린다.
     */
    @Query(
            """
      SELECT MAX(p.requestedAt) FROM PhoneVerification p
      WHERE p.phoneNumber = :phoneNumber
      """)
    Instant findLatestRequestedAt(@Param("phoneNumber") String phoneNumber);

    /**
     * 회원가입이 소비할 후보를 찾는다. 가입토큰은 해시로만 저장하므로 번호로 좁힌 뒤
     * 후보를 하나씩 대조하는 수밖에 없다 — {@code idx_phone_verifications_phone_status} 가
     * 이 조건과 정확히 같다.
     */
    List<PhoneVerification> findByPhoneNumberAndStatus(
            String phoneNumber, PhoneVerificationStatus status);
}
