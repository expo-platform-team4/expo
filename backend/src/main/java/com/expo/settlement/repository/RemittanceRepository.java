package com.expo.settlement.repository;

import com.expo.settlement.entity.Remittance;
import org.springframework.data.jpa.repository.JpaRepository;

/** 송금 기록 영속성 접근 인터페이스. */
public interface RemittanceRepository extends JpaRepository<Remittance, Long> {}
