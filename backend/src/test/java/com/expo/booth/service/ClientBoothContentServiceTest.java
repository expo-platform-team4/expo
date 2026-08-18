package com.expo.booth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expo.booth.converter.BoothContentConverter;
import com.expo.booth.converter.BoothContentFileConverter;
import com.expo.booth.converter.ExternalLinkConverter;
import com.expo.booth.dto.AddBoothContentFileRequest;
import com.expo.booth.dto.AddExternalLinkRequest;
import com.expo.booth.dto.CreateBoothContentRequest;
import com.expo.booth.dto.UpdateBoothContentRequest;
import com.expo.booth.dto.UpdateExternalLinkRequest;
import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothContent;
import com.expo.booth.entity.BoothContentFile;
import com.expo.booth.entity.BoothContentFileType;
import com.expo.booth.entity.ExternalLink;
import com.expo.booth.entity.ExternalLinkType;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothContentFileRepository;
import com.expo.booth.repository.BoothContentRepository;
import com.expo.booth.repository.ExternalLinkRepository;
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
    private static final Long ADMIN_ID = 7L;

    private BoothContentRepository boothContentRepository;
    private BoothContentFileRepository boothContentFileRepository;
    private ExternalLinkRepository externalLinkRepository;
    private BoothAllocationRepository boothAllocationRepository;
    private ClientBoothContentService service;

    @BeforeEach
    void setUp() {
        boothContentRepository = mock(BoothContentRepository.class);
        boothContentFileRepository = mock(BoothContentFileRepository.class);
        externalLinkRepository = mock(ExternalLinkRepository.class);
        boothAllocationRepository = mock(BoothAllocationRepository.class);
        service =
                new ClientBoothContentService(
                        boothContentRepository,
                        boothContentFileRepository,
                        externalLinkRepository,
                        boothAllocationRepository,
                        new BoothContentConverter(
                                new BoothContentFileConverter(), new ExternalLinkConverter()),
                        new BoothContentFileConverter(),
                        new ExternalLinkConverter());
        when(externalLinkRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());
    }

    private BoothAllocation allocation() {
        return BoothAllocation.create(APPLICATION_ID, ORDER_ID, BOOTH_PRODUCT_ID, CLIENT_USER_ID);
    }

    private BoothContent content() {
        return BoothContent.create(
                        ALLOCATION_ID, CLIENT_USER_ID, "회사", "제목", "회사소개", "부스소개", "제품소개")
                .attachImages(null, null);
    }

    /** 검수 요청 → 승인을 거쳐 공개 상태로 만든다. */
    private static void moveToPublished(BoothContent content) {
        content.submitForReview();
        content.approve(ADMIN_ID);
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
        moveToPublished(content);
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
    void submitForReviewRejectsWhenAlreadyPublished() {
        BoothContent content = content();
        moveToPublished(content);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));

        assertThatThrownBy(() -> service.submitForReview(CONTENT_ID, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_SUBMITTABLE);
    }

    /** 검수 요청만으로는 바로 공개되지 않고, 관리자 승인 전 상태(UNDER_REVIEW)로만 넘어가야 한다. */
    @Test
    void submitForReviewSucceedsFromCorrectionRequested() {
        BoothContent content = content();
        moveToPublished(content);
        content.requestCorrection("문구 수정 필요");
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(boothContentFileRepository.findAllByBoothContentIdOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of());

        var response = service.submitForReview(CONTENT_ID, CLIENT_USER_ID);

        assertThat(response.status().name()).isEqualTo("UNDER_REVIEW");
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
        moveToPublished(content);
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
        moveToPublished(content);
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
        moveToPublished(content);
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
        moveToPublished(content);
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

    @Test
    void addLinkSucceeds() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(externalLinkRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        AddExternalLinkRequest request =
                new AddExternalLinkRequest(
                        ExternalLinkType.HOMEPAGE, "홈페이지", "https://example.com", 0);

        var response = service.addLink(CONTENT_ID, request, CLIENT_USER_ID);

        assertThat(response.url()).isEqualTo("https://example.com");
        assertThat(response.linkType()).isEqualTo(ExternalLinkType.HOMEPAGE);
    }

    @Test
    void addLinkRejectsWhenPublished() {
        BoothContent content = content();
        moveToPublished(content);
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        AddExternalLinkRequest request =
                new AddExternalLinkRequest(
                        ExternalLinkType.HOMEPAGE, "홈페이지", "https://example.com", 0);

        assertThatThrownBy(() -> service.addLink(CONTENT_ID, request, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOTH_CONTENT_NOT_EDITABLE);
    }

    @Test
    void removeLinkRejectsWhenNotFound() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(externalLinkRepository.findByIdAndBoothContentId(99L, CONTENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeLink(CONTENT_ID, 99L, CLIENT_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_LINK_NOT_FOUND);
    }

    @Test
    void updateLinkSucceeds() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        ExternalLink link =
                ExternalLink.createForBoothContent(
                        CONTENT_ID, ExternalLinkType.SOCIAL, "인스타", "https://old.example.com", 0);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(externalLinkRepository.findByIdAndBoothContentId(20L, CONTENT_ID))
                .thenReturn(Optional.of(link));
        UpdateExternalLinkRequest request =
                new UpdateExternalLinkRequest(
                        ExternalLinkType.HOMEPAGE, "새 홈페이지", "https://new.example.com");

        var response = service.updateLink(CONTENT_ID, 20L, request, CLIENT_USER_ID);

        assertThat(response.url()).isEqualTo("https://new.example.com");
        assertThat(response.linkType()).isEqualTo(ExternalLinkType.HOMEPAGE);
    }

    @Test
    void reorderLinkSucceeds() {
        BoothContent content = content();
        withId(content, CONTENT_ID);
        ExternalLink link =
                ExternalLink.createForBoothContent(
                        CONTENT_ID, ExternalLinkType.SOCIAL, "인스타", "https://example.com", 0);
        when(boothContentRepository.findByIdAndClientUserId(CONTENT_ID, CLIENT_USER_ID))
                .thenReturn(Optional.of(content));
        when(externalLinkRepository.findByIdAndBoothContentId(20L, CONTENT_ID))
                .thenReturn(Optional.of(link));

        var response = service.reorderLink(CONTENT_ID, 20L, 5, CLIENT_USER_ID);

        assertThat(response.sortOrder()).isEqualTo(5);
    }
}
