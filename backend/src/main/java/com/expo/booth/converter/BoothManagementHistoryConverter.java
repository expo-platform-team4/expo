package com.expo.booth.converter;

import com.expo.booth.dto.BoothManagementHistoryResponse;
import com.expo.booth.entity.BoothManagementHistory;
import org.springframework.stereotype.Component;

@Component
public class BoothManagementHistoryConverter {

    public BoothManagementHistoryResponse toResponse(BoothManagementHistory history) {
        return new BoothManagementHistoryResponse(
                history.getId(),
                history.getActionType(),
                history.getReason(),
                history.getProcessedByAdminId(),
                history.getCreatedAt());
    }
}
