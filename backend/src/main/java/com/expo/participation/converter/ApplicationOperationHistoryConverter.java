package com.expo.participation.converter;

import com.expo.participation.dto.ApplicationOperationHistoryResponse;
import com.expo.participation.entity.ApplicationOperationHistory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationOperationHistoryConverter {

    public ApplicationOperationHistoryResponse toResponse(ApplicationOperationHistory history) {
        return new ApplicationOperationHistoryResponse(
                history.getId(),
                history.getApplicationId(),
                history.getActionType(),
                history.getMessage(),
                history.getProcessedByAdminId(),
                history.getCreatedAt());
    }
}
