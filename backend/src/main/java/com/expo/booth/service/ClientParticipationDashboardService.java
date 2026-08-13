package com.expo.booth.service;

import com.expo.booth.dto.ClientDashboardBoothResponse;
import com.expo.booth.repository.ClientDashboardBoothMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클라이언트 본인 참여 신청 목록 조회 (E-API-008).
 *
 * <p>{@code v_client_dashboard_booths} 뷰는 참여 신청 단계부터 확정 배정까지 전 단계를 담고 있다.
 * {@link ClientBoothDashboardService}(E-API-009)는 이 중 확정 배정 건만 걸러 보여주고, 이 서비스는
 * 필터링 없이 전체(신청·주문·결제·배정 상태 전 단계)를 그대로 보여준다.
 */
@Service
@Transactional(readOnly = true)
public class ClientParticipationDashboardService {

    private final ClientDashboardBoothMapper clientDashboardBoothMapper;

    public ClientParticipationDashboardService(
            ClientDashboardBoothMapper clientDashboardBoothMapper) {
        this.clientDashboardBoothMapper = clientDashboardBoothMapper;
    }

    public List<ClientDashboardBoothResponse> getMyParticipations(Long clientUserId) {
        return clientDashboardBoothMapper.findByClientUserId(clientUserId);
    }
}
