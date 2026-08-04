package com.expo.auth.entity;

import com.expo.auth.Role;
import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** USERS 테이블 — 일반 회원·클라이언트·관리자 공통 계정. */
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(nullable = false, unique = true, length = 50)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", nullable = false, length = 20)
  private AccountStatus accountStatus;

  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Column(name = "phone_verified_at")
  private Instant phoneVerifiedAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "withdrawn_at")
  private Instant withdrawnAt;

  @Column(name = "profile_image_file_id")
  private Long profileImageFileId;

  @Column(name = "profile_image_updated_at")
  private Instant profileImageUpdatedAt;

  protected User() {}

  /** 일반 회원 로컬 회원가입용 계정을 생성한다. */
  public static User createMember(
      String email, String passwordHash, String nickname, String phoneNumber) {
    User user = new User();
    user.email = email;
    user.passwordHash = passwordHash;
    user.nickname = nickname;
    user.role = Role.MEMBER;
    user.accountStatus = AccountStatus.ACTIVE;
    user.phoneNumber = phoneNumber;
    return user;
  }

  /** 클라이언트 로컬 회원가입용 계정을 생성한다. role 은 CLIENT 로 고정한다. */
  public static User createClient(
      String email, String passwordHash, String nickname, String phoneNumber) {
    User user = new User();
    user.email = email;
    user.passwordHash = passwordHash;
    user.nickname = nickname;
    user.role = Role.CLIENT;
    user.accountStatus = AccountStatus.ACTIVE;
    user.phoneNumber = phoneNumber;
    return user;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getNickname() {
    return nickname;
  }

  public Role getRole() {
    return role;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public Instant getPhoneVerifiedAt() {
    return phoneVerifiedAt;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Instant getWithdrawnAt() {
    return withdrawnAt;
  }

  public Long getProfileImageFileId() {
    return profileImageFileId;
  }

  public Instant getProfileImageUpdatedAt() {
    return profileImageUpdatedAt;
  }
}
