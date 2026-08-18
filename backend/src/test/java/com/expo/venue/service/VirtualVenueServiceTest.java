package com.expo.venue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.venue.converter.VirtualVenueConverter;
import com.expo.venue.dto.CreateVirtualVenueRequest;
import com.expo.venue.dto.VirtualVenueResponse;
import com.expo.venue.entity.OperationalStatus;
import com.expo.venue.entity.VirtualVenue;
import com.expo.venue.repository.VirtualVenueRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** {@link VirtualVenueService} 의 이름 중복 검증과 목록 조회를 검증한다. */
class VirtualVenueServiceTest {

    private VirtualVenueRepository virtualVenueRepository;
    private VirtualVenueService service;

    @BeforeEach
    void setUp() {
        virtualVenueRepository = mock(VirtualVenueRepository.class);
        service = new VirtualVenueService(virtualVenueRepository, new VirtualVenueConverter());
    }

    private CreateVirtualVenueRequest request() {
        return new CreateVirtualVenueRequest("코엑스", "서울 강남구", "SEOUL", "설명", null);
    }

    @Test
    void createRejectsDuplicateName() {
        when(virtualVenueRepository.existsByName("코엑스")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VIRTUAL_VENUE_NAME);
        verify(virtualVenueRepository, never()).saveAndFlush(any());
    }

    @Test
    void createSucceedsWithActiveStatus() {
        when(virtualVenueRepository.existsByName("코엑스")).thenReturn(false);
        when(virtualVenueRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VirtualVenueResponse response = service.create(request());

        assertThat(response.name()).isEqualTo("코엑스");
        assertThat(response.operationalStatus()).isEqualTo(OperationalStatus.ACTIVE);
        verify(virtualVenueRepository).saveAndFlush(any());
    }

    /** 사전 중복 검사를 통과해도 동시 삽입으로 제약 위반이 나면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesNameConstraintViolationToDuplicateName() {
        when(virtualVenueRepository.existsByName("코엑스")).thenReturn(false);
        when(virtualVenueRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint"
                                        + " \"virtual_venues_name_key\""));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VIRTUAL_VENUE_NAME);
    }

    /** 이미 장소가 1개 등록돼 있으면 더 등록할 수 없다 (이 플랫폼은 킨텍스 한 곳만 다룬다). */
    @Test
    void createRejectsWhenVenueAlreadyExists() {
        when(virtualVenueRepository.existsByName("코엑스")).thenReturn(false);
        when(virtualVenueRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_LIMIT_EXCEEDED);
        verify(virtualVenueRepository, never()).saveAndFlush(any());
    }

    /** 사전 개수 검사를 통과해도 동시 삽입으로 트리거가 막으면 같은 오류로 변환돼야 한다. */
    @Test
    void createTranslatesLimitTriggerViolationToLimitExceeded() {
        when(virtualVenueRepository.existsByName("코엑스")).thenReturn(false);
        when(virtualVenueRepository.saveAndFlush(any()))
                .thenThrow(
                        new DataIntegrityViolationException("ERROR: virtual_venue_limit_exceeded"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VIRTUAL_VENUE_LIMIT_EXCEEDED);
    }

    /** 이름 중복이 아닌 다른 무결성 위반은 그대로 다시 던져야 한다. */
    @Test
    void createRethrowsUnrelatedConstraintViolation() {
        when(virtualVenueRepository.existsByName("코엑스")).thenReturn(false);
        when(virtualVenueRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("some other constraint"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listReturnsAllVenuesConverted() {
        VirtualVenue venue = VirtualVenue.create("킨텍스", "고양시", "GYEONGGI", null, null);
        when(virtualVenueRepository.findAll()).thenReturn(List.of(venue));

        List<VirtualVenueResponse> responses = service.list();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("킨텍스");
    }
}
