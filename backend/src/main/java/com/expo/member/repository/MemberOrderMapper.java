package com.expo.member.repository;

import com.expo.member.dto.MemberOrderResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** {@code v_member_mypage_orders} 뷰 조회 (A-API-019, A-API-020). */
@Mapper
public interface MemberOrderMapper {

    /** 최신순. */
    List<MemberOrderResponse> findByMemberUserId(Long memberUserId);
}
