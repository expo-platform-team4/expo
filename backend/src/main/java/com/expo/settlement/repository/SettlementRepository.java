package com.expo.settlement.repository;

import com.expo.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

/** 정산 영속성 접근 인터페이스. */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {}
