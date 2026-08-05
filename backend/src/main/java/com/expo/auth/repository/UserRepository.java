package com.expo.auth.repository;

import com.expo.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link User} 엔티티용 Spring Data JPA 리포지토리.
 *
 * <p>{@link com.expo.auth.service.AuthService}에서 회원가입 시 중복 검사·저장에 사용한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 이메일이 이미 등록되어 있는지 확인한다.
   *
   * <p>메서드 이름 규칙({@code existsBy + 필드명})으로 Spring Data JPA가 아래 SQL을 자동 생성한다.
   *
   * <pre>{@code SELECT COUNT(u) > 0 FROM User u WHERE u.email = ?1}</pre>
   *
   * @param email 검사할 이메일
   * @return 존재하면 {@code true} (회원가입 시 {@link com.expo.auth.exception.ErrorCode#DUPLICATE_EMAIL})
   */
  boolean existsByEmail(String email);

  /**
   * 닉네임이 이미 사용 중인지 확인한다.
   *
   * <p>{@link User} 엔티티의 {@code nickname} 컬럼(unique) 기준으로 조회한다.
   *
   * @param nickname 검사할 닉네임
   * @return 존재하면 {@code true} (회원가입 시 {@link com.expo.auth.exception.ErrorCode#DUPLICATE_NICKNAME})
   */
  boolean existsByNickname(String nickname);
}
