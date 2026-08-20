package com.expo.expo.repository;

import com.expo.expo.dto.PublicExpoCardRow;
import com.expo.expo.dto.PublicExpoDetailRow;
import com.expo.expo.dto.PublicExpoQuery;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공개 박람회 목록·상세 (이슈 #107).
 *
 * <p>{@code v_public_expo_cards} 뷰가 V1 부터 있었는데 이를 읽는 코드가 하나도 없어서 목록 화면이 "준비 중" 이었다. 뷰가 이미
 * {@code visibility_status = 'PUBLIC'} 으로 걸러 주므로 비공개 박람회는 여기로 나오지 않는다.
 */
@Mapper
public interface PublicExpoMapper {

    List<PublicExpoCardRow> findCards(@Param("query") PublicExpoQuery query);

    Optional<PublicExpoDetailRow> findDetail(@Param("expoId") Long expoId);
}
