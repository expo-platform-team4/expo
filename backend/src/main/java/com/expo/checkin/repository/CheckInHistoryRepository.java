package com.expo.checkin.repository;

import com.expo.checkin.entity.CheckInHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 체크인 이력 영속성 접근 인터페이스. */
public interface CheckInHistoryRepository extends JpaRepository<CheckInHistory, Long> {}
