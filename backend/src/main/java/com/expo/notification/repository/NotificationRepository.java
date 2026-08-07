package com.expo.notification.repository;

import com.expo.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/** 알림 작업 영속성 접근 인터페이스. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {}
