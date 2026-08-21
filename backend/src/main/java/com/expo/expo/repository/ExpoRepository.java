package com.expo.expo.repository;

import com.expo.expo.entity.Expo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoRepository extends JpaRepository<Expo, Long> {

    /** 개최 신청 목록에 "승인으로 만들어진 박람회 ID" 를 붙일 때 쓴다. */
    List<Expo> findByOpeningRequestIdIn(Collection<Long> openingRequestIds);
}
