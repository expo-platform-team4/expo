package com.expo.settlement.service;

import com.expo.settlement.dto.ClientDashboardDailySalesResponse;
import com.expo.settlement.repository.ClientDashboardSettlementMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클라이언트 본인 박람회 판매 현황 조회 (E-API-003). */
@Service
@Transactional(readOnly = true)
public class ClientSettlementDashboardService {

    private final ClientDashboardSettlementMapper clientDashboardSettlementMapper;

    public ClientSettlementDashboardService(
            ClientDashboardSettlementMapper clientDashboardSettlementMapper) {
        this.clientDashboardSettlementMapper = clientDashboardSettlementMapper;
    }

    public List<ClientDashboardDailySalesResponse> getMyDailySales(Long clientUserId, Long expoId) {
        return clientDashboardSettlementMapper.findDailySalesByClientUserIdAndExpoId(
                clientUserId, expoId);
    }
}
