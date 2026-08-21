package com.expo.auth.repository;

import com.expo.auth.entity.EmailVerification;
import com.expo.auth.entity.EmailVerificationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@link EmailVerification} 리포지토리. */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
      UPDATE EmailVerification e
      SET e.status = :expiredStatus
      WHERE e.email = :email
        AND e.status = :requestedStatus
      """)
    int expireRequestedByEmail(
            @Param("email") String email,
            @Param("requestedStatus") EmailVerificationStatus requestedStatus,
            @Param("expiredStatus") EmailVerificationStatus expiredStatus);

    /**
     * 회원가입이 토큰을 소비할 때 후보를 찾는다. 토큰 원문은 저장하지 않으므로(해시만 저장) 이메일과
     * 상태로 후보를 좁힌 뒤, 서비스 쪽에서 {@code PasswordEncoder#matches} 로 하나씩 대조한다 —
     * {@code PasswordResetService#confirmReset} 과 같은 패턴이다.
     */
    List<EmailVerification> findByEmailAndStatus(String email, EmailVerificationStatus status);
}
