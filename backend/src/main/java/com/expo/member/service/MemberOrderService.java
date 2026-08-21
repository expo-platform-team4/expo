package com.expo.member.service;

import com.expo.member.dto.MemberOrderResponse;
import com.expo.member.repository.MemberOrderMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 마이페이지 "예매 내역" 조회 (A-API-019, A-API-020). */
@Service
@Transactional(readOnly = true)
public class MemberOrderService {

    private final MemberOrderMapper memberOrderMapper;

    public MemberOrderService(MemberOrderMapper memberOrderMapper) {
        this.memberOrderMapper = memberOrderMapper;
    }

    /**
     * 로그인한 회원 본인의 주문을 최신순으로 전부 돌려준다.
     *
     * <p>목록·상세를 API 를 나누지 않는다 — 뷰 자체가 이미 주문 하나당 필요한 정보를 전부 담고 있어(결제·환불
     * 최신 상태, 환불 가능 여부 포함), 상세 화면이 따로 더 조회할 게 없다.
     */
    public List<MemberOrderResponse> getMyOrders(Long memberUserId) {
        return memberOrderMapper.findByMemberUserId(memberUserId);
    }
}
