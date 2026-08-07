package com.expo.expo.service;

import com.expo.expo.dto.ClientDashboardExpoResponse;
import com.expo.expo.repository.ClientDashboardExpoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클라이언트 본인 등록 박람회 목록 조회 (E-API-002). */
@Service
@Transactional(readOnly = true)
public class ClientExpoDashboardService {

    private final ClientDashboardExpoMapper clientDashboardExpoMapper;

    public ClientExpoDashboardService(ClientDashboardExpoMapper clientDashboardExpoMapper) {
        this.clientDashboardExpoMapper = clientDashboardExpoMapper;
    }

    public List<ClientDashboardExpoResponse> getMyExpos(Long clientUserId) {
        return clientDashboardExpoMapper.findByClientUserId(clientUserId);
    }
}
