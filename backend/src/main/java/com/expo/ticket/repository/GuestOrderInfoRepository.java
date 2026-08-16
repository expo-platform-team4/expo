package com.expo.ticket.repository;

import com.expo.ticket.entity.GuestOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestOrderInfoRepository extends JpaRepository<GuestOrder, Long> {}
