package com.expo.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * CLIENT_PROFILES — role=CLIENT 인 사용자의 사업자 상세 프로필.
 *
 * <p>ERD v16 의 CLIENT_PROFILES 규격을 그대로 매핑한다. 이 작업(A-API-005)에서는 사업자등록번호 존재 여부 조회만 필요하지만, {@code
 * ddl-auto: validate} 환경에서 부팅이 실패하지 않도록 NOT NULL 컬럼을 모두 매핑해 둔다.
 *
 * <p>회원가입 저장 로직(A-API-002)은 이 작업 범위가 아니므로 setter 나 factory 는 두지 않는다.
 */
@Entity
@Table(name = "client_profiles")
public class ClientProfile {

  /** USERS.id 와 동일한 값을 PK 로 사용한다(1:1). FK 매핑은 회원가입 API 구현 시점에 채운다. */
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "business_number", nullable = false, unique = true, length = 20)
  private String businessNumber;

  @Column(name = "company_name", nullable = false, length = 150)
  private String companyName;

  @Column(name = "representative_name", nullable = false, length = 100)
  private String representativeName;

  @Column(name = "business_address", nullable = false, length = 255)
  private String businessAddress;

  @Column(name = "business_type", length = 100)
  private String businessType;

  @Column(name = "business_number_verified", nullable = false)
  private boolean businessNumberVerified;

  @Column(name = "business_verified_at")
  private Instant businessVerifiedAt;

  @Column(name = "verification_provider", length = 30)
  private String verificationProvider;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ClientProfile() {}

  public Long getUserId() {
    return userId;
  }

  public String getBusinessNumber() {
    return businessNumber;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getRepresentativeName() {
    return representativeName;
  }

  public String getBusinessAddress() {
    return businessAddress;
  }

  public String getBusinessType() {
    return businessType;
  }

  public boolean isBusinessNumberVerified() {
    return businessNumberVerified;
  }

  public Instant getBusinessVerifiedAt() {
    return businessVerifiedAt;
  }

  public String getVerificationProvider() {
    return verificationProvider;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
