package com.expo.member.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.member.dto.ClientDashboardProfileResponse;
import com.expo.member.repository.ClientDashboardProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클라이언트 마이페이지 요약(상단 프로필) 조회 (E-API-001). */
@Service
@Transactional(readOnly = true)
public class ClientDashboardService {

    private final ClientDashboardProfileMapper clientDashboardProfileMapper;

    public ClientDashboardService(ClientDashboardProfileMapper clientDashboardProfileMapper) {
        this.clientDashboardProfileMapper = clientDashboardProfileMapper;
    }

    /**
     * 로그인한 클라이언트 본인의 마이페이지 요약(프로필)을 조회한다.
     *
     * @throws BusinessException 클라이언트 프로필이 없는 경우 (정상 흐름에서는 발생하지 않아야 함, {@code
     *     CLIENT_PROFILE_NOT_FOUND})
     */
    public ClientDashboardProfileResponse getDashboardSummary(Long clientUserId) {
        ClientDashboardProfileResponse response =
                clientDashboardProfileMapper.findByClientUserId(clientUserId);
        if (response == null) {
            throw new BusinessException(ErrorCode.CLIENT_PROFILE_NOT_FOUND);
        }
        return response;
    }
}
