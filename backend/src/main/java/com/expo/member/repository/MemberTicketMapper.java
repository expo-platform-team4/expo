package com.expo.member.repository;

import com.expo.member.dto.MemberTicketRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** {@code v_member_mypage_tickets} 뷰 조회 (A-API-021, A-API-022). */
@Mapper
public interface MemberTicketMapper {

    /** 박람회별로 묶기 전, 평평한 로우 그대로. 정렬은 서비스에서 그룹핑할 때 기준을 잡는다. */
    List<MemberTicketRow> findByMemberUserId(Long memberUserId);
}
