package com.expo.settlement.repository;

import com.expo.settlement.dto.ClientDashboardDailySalesResponse;
import com.expo.settlement.dto.ClientDashboardSettlementResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientDashboardSettlementMapper {

    List<ClientDashboardDailySalesResponse> findDailySalesByClientUserIdAndExpoId(
            Long clientUserId, Long expoId);

    List<ClientDashboardSettlementResponse> findSettlementsByHostClientId(Long hostClientId);
}
