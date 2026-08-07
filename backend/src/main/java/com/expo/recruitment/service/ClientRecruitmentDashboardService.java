package com.expo.recruitment.service;

import com.expo.recruitment.dto.ClientDashboardRecruitmentResponse;
import com.expo.recruitment.repository.ClientDashboardRecruitmentMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클라이언트 본인 모집공고 결과 목록 조회 (E-API-005). */
@Service
@Transactional(readOnly = true)
public class ClientRecruitmentDashboardService {

    private final ClientDashboardRecruitmentMapper clientDashboardRecruitmentMapper;

    public ClientRecruitmentDashboardService(
            ClientDashboardRecruitmentMapper clientDashboardRecruitmentMapper) {
        this.clientDashboardRecruitmentMapper = clientDashboardRecruitmentMapper;
    }

    public List<ClientDashboardRecruitmentResponse> getMyRecruitmentNotices(Long hostClientId) {
        return clientDashboardRecruitmentMapper.findByHostClientId(hostClientId);
    }
}
