package com.expo.booth.service;

import com.expo.booth.dto.ClientDashboardBoothResponse;
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
     * 확정 배정(부스 배정 ID가 존재하는) 건만 필터링해 반환한다.
     *
     * <p>{@code v_client_dashboard_booths} 뷰는 참여 신청 단계부터 전체를 반환하므로, "확정 배정" 조건은 여기서
     * {@code boothAllocationId != null}로 좁힌다.
     */
    public List<ClientDashboardBoothResponse> getMyConfirmedBooths(Long clientUserId) {
        return clientDashboardBoothMapper.findByClientUserId(clientUserId).stream()
                .filter(booth -> booth.boothAllocationId() != null)
                .toList();
    }
}
