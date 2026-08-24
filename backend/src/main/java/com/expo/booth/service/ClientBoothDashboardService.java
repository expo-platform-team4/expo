package com.expo.booth.service;

import com.expo.booth.dto.ClientDashboardBoothResponse;
import com.expo.booth.entity.BoothAllocationStatus;
import com.expo.booth.repository.ClientDashboardBoothMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클라이언트 본인 확정 배정 부스 목록 조회 (E-API-009). */
@Service
@Transactional(readOnly = true)
public class ClientBoothDashboardService {

    private final ClientDashboardBoothMapper clientDashboardBoothMapper;

    public ClientBoothDashboardService(ClientDashboardBoothMapper clientDashboardBoothMapper) {
        this.clientDashboardBoothMapper = clientDashboardBoothMapper;
    }

    /**
     * 확정 배정(부스 배정이 존재하고 아직 ASSIGNED 상태인) 건만 필터링해 반환한다.
     *
     * <p>{@code v_client_dashboard_booths} 뷰는 참여 신청 단계부터 전체를 반환하고 배정이 취소돼도 행 자체는
     * 그대로 남으므로, {@code boothAllocationId != null} 만으로 거르면 취소된 배정도 "확정 배정 부스"로 계속
     * 노출된다. 배정 상태(ASSIGNED)까지 같이 확인해야 한다. 같은 매퍼를 쓰는 {@code
     * ClientParticipationDashboardService} 는 전체 진행 현황을 보여주는 용도라 이 필터를 적용하면 안 되므로,
     * 쿼리(뷰)가 아니라 이 서비스에서만 좁힌다.
     */
    public List<ClientDashboardBoothResponse> getMyConfirmedBooths(Long clientUserId) {
        return clientDashboardBoothMapper.findByClientUserId(clientUserId).stream()
                .filter(booth -> booth.boothAllocationId() != null)
                .filter(
                        booth ->
                                BoothAllocationStatus.ASSIGNED
                                        .name()
                                        .equals(booth.allocationStatus()))
                .toList();
    }
}
