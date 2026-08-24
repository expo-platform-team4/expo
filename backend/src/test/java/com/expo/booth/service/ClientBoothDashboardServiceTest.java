package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.booth.dto.ClientDashboardBoothResponse;
import com.expo.booth.repository.ClientDashboardBoothMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link ClientBoothDashboardService} 의 확정 배정 필터링 규칙을 확인한다. */
class ClientBoothDashboardServiceTest {

    private static final Long CLIENT_USER_ID = 1L;

    private final ClientDashboardBoothMapper clientDashboardBoothMapper =
            mock(ClientDashboardBoothMapper.class);
    private final ClientBoothDashboardService service =
            new ClientBoothDashboardService(clientDashboardBoothMapper);

    private ClientDashboardBoothResponse row(Long allocationId, String allocationStatus) {
        return new ClientDashboardBoothResponse(
                CLIENT_USER_ID,
                10L,
                20L,
                "SUBMITTED",
                30L,
                "PAYMENT_COMPLETED",
                "APPROVED",
                Instant.now(),
                allocationId,
                allocationStatus,
                "A-01");
    }

    @Test
    void excludesRowsWithoutAllocation() {
        when(clientDashboardBoothMapper.findByClientUserId(CLIENT_USER_ID))
                .thenReturn(List.of(row(null, null)));

        var result = service.getMyConfirmedBooths(CLIENT_USER_ID);

        assertThat(result).isEmpty();
    }

    /** 배정이 관리자 직권으로 취소돼도 뷰의 행 자체는 남으므로, 배정 상태까지 확인해서 걸러야 한다. */
    @Test
    void excludesCanceledAllocations() {
        when(clientDashboardBoothMapper.findByClientUserId(CLIENT_USER_ID))
                .thenReturn(List.of(row(40L, "CANCELED")));

        var result = service.getMyConfirmedBooths(CLIENT_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void includesAssignedAllocations() {
        when(clientDashboardBoothMapper.findByClientUserId(CLIENT_USER_ID))
                .thenReturn(List.of(row(40L, "ASSIGNED")));

        var result = service.getMyConfirmedBooths(CLIENT_USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).boothAllocationId()).isEqualTo(40L);
    }
}
