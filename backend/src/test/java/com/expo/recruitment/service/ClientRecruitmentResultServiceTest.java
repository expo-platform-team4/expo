package com.expo.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.recruitment.converter.RecruitmentResultConverter;
import com.expo.recruitment.converter.RecruitmentResultItemConverter;
import com.expo.recruitment.entity.RecruitmentResult;
import com.expo.recruitment.entity.RecruitmentResultStatus;
import com.expo.recruitment.repository.RecruitmentResultItemRepository;
import com.expo.recruitment.repository.RecruitmentResultRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link ClientRecruitmentResultService} 의 조회·확인 규칙을 확인한다. */
class ClientRecruitmentResultServiceTest {

    private static final Long NOTICE_ID = 1L;
    private static final Long HOST_CLIENT_ID = 2L;
    private static final Long RESULT_ID = 3L;

    private RecruitmentResultRepository recruitmentResultRepository;
    private RecruitmentResultItemRepository recruitmentResultItemRepository;
    private ClientRecruitmentResultService service;

    @BeforeEach
    void setUp() {
        recruitmentResultRepository = mock(RecruitmentResultRepository.class);
        recruitmentResultItemRepository = mock(RecruitmentResultItemRepository.class);
        service =
                new ClientRecruitmentResultService(
                        recruitmentResultRepository,
                        recruitmentResultItemRepository,
                        new RecruitmentResultConverter(new RecruitmentResultItemConverter()));
        when(recruitmentResultItemRepository.findAllByRecruitmentResultId(any()))
                .thenReturn(List.of());
    }

    @Test
    void getMineRejectsWhenNotOwned() {
        when(recruitmentResultRepository.findByIdAndHostClientId(RESULT_ID, HOST_CLIENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(RESULT_ID, HOST_CLIENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_RESULT_NOT_FOUND);
    }

    @Test
    void confirmRejectsWhenNotDelivered() {
        RecruitmentResult result =
                RecruitmentResult.create(NOTICE_ID, HOST_CLIENT_ID, 0, 0, BigDecimal.ZERO);
        when(recruitmentResultRepository.findByIdAndHostClientIdForUpdate(
                        RESULT_ID, HOST_CLIENT_ID))
                .thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.confirm(RESULT_ID, HOST_CLIENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECRUITMENT_RESULT_NOT_CONFIRMABLE);
    }

    @Test
    void confirmSucceedsWhenDelivered() {
        RecruitmentResult result =
                RecruitmentResult.create(NOTICE_ID, HOST_CLIENT_ID, 0, 0, BigDecimal.ZERO);
        result.deliver();
        when(recruitmentResultRepository.findByIdAndHostClientIdForUpdate(
                        RESULT_ID, HOST_CLIENT_ID))
                .thenReturn(Optional.of(result));

        var response = service.confirm(RESULT_ID, HOST_CLIENT_ID);

        assertThat(response.status()).isEqualTo(RecruitmentResultStatus.CONFIRMED);
    }
}
