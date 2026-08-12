package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothContentFileConverter;
import com.expo.booth.dto.AddBoothContentFileRequest;
import com.expo.booth.dto.CreateBoothContentRequest;
import com.expo.booth.dto.UpdateBoothContentRequest;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.BoothContentFileType;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link ClientBoothContentService} 의 작성·수정·공개 규칙을 확인한다. */
class ClientBoothContentServiceTest {

    private static final Long ALLOCATION_ID = 1L;
    private static final Long CONTENT_ID = 2L;
    private static final Long CLIENT_USER_ID = 3L;
    private static final Long APPLICATION_ID = 4L;
    private static final Long ORDER_ID = 5L;
    private static final Long BOOTH_PRODUCT_ID = 6L;

    private BoothContentRepository boothContentRepository;
    private BoothContentFileRepository boothContentFileRepository;
    private BoothAllocationRepository boothAllocationRepository;
    private ClientBoothContentService service;

    @BeforeEach
    void setUp() {
        boothContentRepository = mock(BoothContentRepository.class);
        boothContentFileRepository = mock(BoothContentFileRepository.class);
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        service =
                new ClientBoothContentService(
                        boothContentRepository,
                        boothContentFileRepository,
                        boothAllocationRepository,
                        new BoothContentConverter(new BoothContentFileConverter()),
                        new BoothContentFileConverter());
    }

    private BoothAllocation allocation() {
        return BoothAllocation.create(APPLICATION_ID, ORDER_ID, BOOTH_PRODUCT_ID, CLIENT_USER_ID);
    }

    private BoothContent content() {
        return BoothContent.create(
                        ALLOCATION_ID, CLIENT_USER_ID, "회사", "제목", "회사소개", "부스소개", "제품소개")
                .attachImages(null, null);
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
    void createRejectsWhenAllocationNotOwned() {
        when(boothAllocationRepository.findByIdAndClientUserId(ALLOCATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.empty());
        CreateBoothContentRequest request =
                new CreateBoothContentRequest(
                        ALLOCATION_ID, "회사", "제목", "회사소개", "부스소개", "제품소개", null, null);

        assertThatThrownBy(() -> service.create(request, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_FOUND);
    }

    @Test
    void createSucceeds() {
        BoothAllocation allocation = allocation();
        when(boothAllocationRepository.findByIdAndClientUserId(ALLOCATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(allocation));
        when(boothContentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateBoothContentRequest request =
                new CreateBoothContentRequest(
                        ALLOCATION_ID, "회사", "제목", "회사소개", "부스소개", "제품소개", null, null);

        var response = service.create(request, CLIENT_USER_ID);

        assertThat(response.companyDisplayName()).isEqualTo("회사");
        assertThat(response.status().name()).isEqualTo("DRAFT");
    }

    @Test
    void createRejectsWhenAllocationNotAssigned() {
        BoothAllocation allocation = allocation();
        allocation.cancel("이중 배정 정정");
        when(boothAllocationRepository.findByIdAndClientUserId(ALLOCATION_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(allocation));
        CreateBoothContentRequest request =
                new CreateBoothContentRequest(
                        ALLOCATION_ID, "회사", "제목", "회사소개", "부스소개", "제품소개", null, null);

        assertThatThrownBy(() -> service.create(request, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_ALLOCATION_NOT_ASSIGNED);
    }

    @Test
    void updateRejectsWhenPublished() {
        BoothContent content = content();
        content.publish();
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        UpdateBoothContentRequest request =
                new UpdateBoothContentRequest("새 회사", "새 제목", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(CONTENT_ID, request, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
    }

    @Test
    void updateSucceedsWhenDraft() {
        BoothContent content = content();
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());
        UpdateBoothContentRequest request =
                new UpdateBoothContentRequest("새 회사", "새 제목", null, null, null, null, null);

        var response = service.update(CONTENT_ID, request, CLIENT_USER_ID);

        assertThat(response.companyDisplayName()).isEqualTo("새 회사");
    }

    @Test
    void publishRejectsWhenAlreadyPublished() {
        BoothContent content = content();
        content.publish();
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.publish(CONTENT_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_PUBLISHABLE);
    }

    @Test
    void publishSucceedsFromCorrectionRequested() {
        BoothContent content = content();
        content.publish();
        content.requestCorrection("문구 수정 필요");
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());

        var response = service.publish(CONTENT_ID, CLIENT_USER_ID);

        assertThat(response.status().name()).isEqualTo("PUBLISHED");
        assertThat(response.correctionRequestedAt()).isNull();
        assertThat(response.correctionMessage()).isNull();
    }

    @Test
    void getPublishedRejectsWhenNotPublished() {
        when(boothContentRepository.findByBoothAllocationId(ALLOCATION_ID))
                .thenReturn(Optional.of(content()));

        assertThatThrownBy(() -> service.getPublished(ALLOCATION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_FOUND);
    }

    @Test
    void getPublishedSucceeds() {
        BoothContent content = content();
        content.publish();
        when(boothContentRepository.findByBoothAllocationId(ALLOCATION_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());

        var response = service.getPublished(ALLOCATION_ID);

        assertThat(response.companyDisplayName()).isEqualTo("회사");
    }

    @Test
    void addFileSucceeds() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        AddBoothContentFileRequest request =
                new AddBoothContentFileRequest(10L, BoothContentFileType.GALLERY_IMAGE, "이미지", 0);

        var response = service.addFile(CONTENT_ID, request, CLIENT_USER_ID);

        assertThat(response.fileId()).isEqualTo(10L);
    }

    @Test
    void addFileRejectsWhenPublished() {
        BoothContent content = content();
        content.publish();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        AddBoothContentFileRequest request =
                new AddBoothContentFileRequest(10L, BoothContentFileType.GALLERY_IMAGE, "이미지", 0);

        assertThatThrownBy(() -> service.addFile(CONTENT_ID, request, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
    }

    @Test
    void removeFileRejectsWhenPublished() {
        BoothContent content = content();
        content.publish();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.removeFile(CONTENT_ID, 20L, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
    }

    @Test
    void reorderFileRejectsWhenPublished() {
        BoothContent content = content();
        content.publish();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.reorderFile(CONTENT_ID, 20L, 3, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
    }

    @Test
    void removeFileRejectsWhenNotFound() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.findByIdAndBoothContentId(99L, CONTENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeFile(CONTENT_ID, 99L, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_FILE_NOT_FOUND);
    }

    @Test
    void reorderFileSucceeds() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        BoothContentFile file =
                BoothContentFile.create(
                        CONTENT_ID, 10L, BoothContentFileType.GALLERY_IMAGE, "이미지", 0);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.findByIdAndBoothContentId(20L, CONTENT_ID))
                .thenReturn(Optional.of(file));

        var response = service.reorderFile(CONTENT_ID, 20L, 3, CLIENT_USER_ID);

        assertThat(response.sortOrder()).isEqualTo(3);
    }
}
