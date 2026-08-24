package com.expo.banner.repository;

import com.expo.banner.entity.BannerSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerSlotRepository extends JpaRepository<BannerSlot, Long> {

    Optional<BannerSlot> findBySlotCode(String slotCode);

    List<BannerSlot> findByActiveTrue();
}
