package com.expo.auth.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * CLIENT_PROFILES — role=CLIENT 인 사용자의 사업자 상세 프로필.
 *
 * <p>ERD v16 의 CLIENT_PROFILES 규격을 그대로 매핑한다. 이 작업(A-API-005)에서는 사업자등록번호 존재 여부 조회만 필요하지만, {@code
 * ddl-auto: validate} 환경에서 부팅이 실패하지 않도록 NOT NULL 컬럼을 모두 매핑해 둔다.
 */
@Entity
@Table(name = "client_profiles")
public class ClientProfile extends BaseTimeEntity {

  /**
   * MVP 회원가입 화면에 없는 NOT NULL 컬럼용 임시값.
   *
   * <p>마이그레이션·ERD는 NOT NULL 이지만 프론트는 아직 대표자명·사업장 주소를 받지 않으므로, 스키마 변경 없이 DB 제약을 만족시킨다. 추후 프로필 수정
   * API에서 실제 값으로 갱신한다.
   */
  private static final String MVP_PLACEHOLDER = "미입력";

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

  protected ClientProfile() {}

  /**
   * 클라이언트 회원가입 시 사업자 프로필을 생성한다.
   *
   * <p>MVP 테스트 환경에서는 실제 국세청 API 를 호출하지 않으므로 {@code businessNumberVerified} 는 {@code false} 로 저장한다.
   *
   * <p>대표자명·사업장 주소는 화면에 없으므로 {@link #MVP_PLACEHOLDER} 로 채운다.
   */
  public static ClientProfile create(Long userId, String businessNumber, String companyName) {
    ClientProfile profile = new ClientProfile();
    profile.userId = userId;
    profile.businessNumber = businessNumber;
    profile.companyName = companyName;
    profile.representativeName = MVP_PLACEHOLDER;
    profile.businessAddress = MVP_PLACEHOLDER;
    profile.businessNumberVerified = false;
    return profile;
  }

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
}
