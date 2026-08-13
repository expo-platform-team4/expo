package com.expo.settlement.service;

import com.expo.checkin.service.ExpoHostVerifier;
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
    private final ExpoHostVerifier expoHostVerifier;

    public ClientSettlementDashboardService(
            ClientDashboardSettlementMapper clientDashboardSettlementMapper,
            ExpoHostVerifier expoHostVerifier) {
        this.clientDashboardSettlementMapper = clientDashboardSettlementMapper;
        this.expoHostVerifier = expoHostVerifier;
    }

    /**
     * @throws com.expo.common.exception.BusinessException 박람회가 없거나({@code EXPO_NOT_FOUND}),
     *     본인 소유가 아니면({@code NOT_EXPO_HOST})
     */
    public List<ClientDashboardDailySalesResponse> getMyDailySales(Long clientUserId, Long expoId) {
        expoHostVerifier.verifyHost(expoId, clientUserId);
        return clientDashboardSettlementMapper.findDailySalesByClientUserIdAndExpoId(
                clientUserId, expoId);
    }
}
