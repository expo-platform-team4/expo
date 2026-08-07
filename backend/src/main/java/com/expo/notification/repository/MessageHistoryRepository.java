package com.expo.notification.repository;

import com.expo.notification.entity.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 발송 시도 이력 영속성 접근 인터페이스. */
public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {}
