package com.expo.auth.repository;

import com.expo.auth.entity.PhoneVerification;
import com.expo.auth.entity.PhoneVerificationStatus;
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
}
