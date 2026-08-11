package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothContentFileConverter;
import com.expo.booth.converter.BoothManagementHistoryConverter;
import com.expo.booth.converter.ExternalLinkConverter;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.BoothManagementHistoryRepository;
import com.expo.booth.repository.ExternalLinkRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link AdminBoothContentService} 의 운영 확인·보완 요청·숨김 규칙을 확인한다. */
class AdminBoothContentServiceTest {

    private static final Long ALLOCATION_ID = 1L;
    private static final Long CONTENT_ID = 2L;
    private static final Long CLIENT_USER_ID = 3L;
    private static final Long ADMIN_ID = 99L;

    private BoothContentRepository boothContentRepository;
    private BoothContentFileRepository boothContentFileRepository;
    private ExternalLinkRepository externalLinkRepository;
    private BoothManagementHistoryRepository boothManagementHistoryRepository;
    private AdminBoothContentService service;

    @BeforeEach
    void setUp() {
        boothContentRepository = mock(BoothContentRepository.class);
        boothContentFileRepository = mock(BoothContentFileRepository.class);
        externalLinkRepository = mock(ExternalLinkRepository.class);
        boothManagementHistoryRepository = mock(BoothManagementHistoryRepository.class);
        service =
                new AdminBoothContentService(
                        boothContentRepository,
                        boothContentFileRepository,
                        externalLinkRepository,
                        boothManagementHistoryRepository,
                        new BoothContentConverter(
                                new BoothContentFileConverter(), new ExternalLinkConverter()),
                        new BoothManagementHistoryConverter());
        when(boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());
        when(externalLinkRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());
    }

    private BoothContent content() {
        BoothContent content =
                BoothContent.create(ALLOCATION_ID, CLIENT_USER_ID, "회사", "제목", null, null, null)
                        .attachImages(null, null);
        withId(content, CONTENT_ID);
        return content;
    }

    private static void withId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void getRejectsWhenNotFound() {
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(CONTENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_FOUND);
    }

    @Test
    void checkRecordsAdminWithoutHistory() {
        BoothContent content = content();
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        var response = service.check(CONTENT_ID, ADMIN_ID);

        assertThat(response.checkedByAdminId()).isEqualTo(ADMIN_ID);
        verify(boothManagementHistoryRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void requestCorrectionMovesToCorrectionRequestedAndLogsHistory() {
        BoothContent content = content();
        content.publish();
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        var response = service.requestCorrection(CONTENT_ID, ADMIN_ID, "문구 확인 필요");

        assertThat(response.status().name()).isEqualTo("CORRECTION_REQUESTED");
        verify(boothManagementHistoryRepository).save(any());
    }

    @Test
    void hideMovesToHiddenAndLogsHistory() {
        BoothContent content = content();
        content.publish();
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        var response = service.hide(CONTENT_ID, ADMIN_ID, "정책 위반");

        assertThat(response.status().name()).isEqualTo("HIDDEN");
        verify(boothManagementHistoryRepository).save(any());
    }

    @Test
    void hideRejectsWhenDraft() {
        BoothContent content = content();
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.hide(CONTENT_ID, ADMIN_ID, "정책 위반"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_HIDABLE);
    }

    @Test
    void restoreRejectsWhenNotHidden() {
        BoothContent content = content();
        content.publish();
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.restore(CONTENT_ID, ADMIN_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_RESTORABLE);
    }

    @Test
    void restoreSucceedsFromHidden() {
        BoothContent content = content();
        content.publish();
        content.hide();
        when(boothContentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        var response = service.restore(CONTENT_ID, ADMIN_ID, "재검토 완료");

        assertThat(response.status().name()).isEqualTo("PUBLISHED");
        verify(boothManagementHistoryRepository).save(any());
    }
}
