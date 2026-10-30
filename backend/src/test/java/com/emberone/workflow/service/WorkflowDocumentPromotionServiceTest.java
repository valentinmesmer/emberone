/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.userdocument.service.UserDocumentPromotionService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowComment;
import com.emberone.workflow.domain.WorkflowMediaSource;
import com.emberone.workflow.dto.WorkflowDocumentPromotionDefaults;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.dto.WorkflowDocumentPromotionSelection;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowCommentRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowDocumentPromotionServiceTest {

    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowCommentRepository commentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock UserDocumentPromotionService userDocumentPromotionService;
    @Mock WorkflowHandler<Object> handler;

    private WorkflowDocumentPromotionService service;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        service =
                new WorkflowDocumentPromotionService(
                        attachmentRepository,
                        commentRepository,
                        workflowDocumentService,
                        userDocumentPromotionService,
                        JsonMapper.builder().build());
        workflow = Workflow.builder().id(5L).tenantId(7L).openedByUserId(1L).build();
        ReflectionTestUtils.setField(service, "workflowsBucket", "workflows");
        lenient().when(handler.allowsDocumentPromotion(workflow)).thenReturn(true);
        lenient()
                .when(handler.resolveDocumentPromotionDefaults(workflow))
                .thenReturn(
                        WorkflowDocumentPromotionDefaults.builder()
                                .includeWorkflowMedia(true)
                                .includeCommentMedia(false)
                                .build());
        lenient().when(commentRepository.findByWorkflow_IdOrderByIdAsc(5L)).thenReturn(List.of());
        lenient()
                .when(workflowDocumentService.listCommentAttachments(workflow, List.of()))
                .thenReturn(java.util.Map.of());
    }

    @Test
    void previewMarksWorkflowAttachmentSelectedByHandlerDefault() {
        EntityAttachmentDto attachment =
                EntityAttachmentDto.builder().id(10L).fileName("proof.pdf").build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(attachment));
        when(handler.resolveDocumentPromotionMetadata(workflow)).thenReturn(Optional.empty());

        var preview = service.buildPreview(workflow, handler);

        assertThat(preview.getItems())
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.getAttachmentId()).isEqualTo(10L);
                            assertThat(item.getSource()).isEqualTo(WorkflowMediaSource.WORKFLOW);
                            assertThat(item.isSelected()).isTrue();
                            assertThat(item.getDefaultTitle()).isEqualTo("proof.pdf");
                        });
    }

    @Test
    void pendingSelectionsOverrideDefaultsAndAreEnrichedBeforeStorage() {
        EntityAttachmentDto attachment =
                EntityAttachmentDto.builder().id(10L).fileName("proof.pdf").build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(attachment));
        when(handler.resolveDocumentPromotionMetadata(workflow)).thenReturn(Optional.empty());
        WorkflowDocumentPromotionSelection selection =
                WorkflowDocumentPromotionSelection.builder()
                        .attachmentId(10L)
                        .source(WorkflowMediaSource.WORKFLOW)
                        .title(" Custom title ")
                        .build();

        var preview =
                service.savePendingSelections(workflow, handler, List.of(selection, selection));

        assertThat(workflow.getPendingDocumentPromotions()).contains("proof.pdf");
        assertThat(service.readPendingSelections(workflow))
                .singleElement()
                .satisfies(
                        stored -> {
                            assertThat(stored.getTitle()).isEqualTo("proof.pdf");
                            assertThat(stored.getTags()).isEmpty();
                        });
        assertThat(preview.getItems())
                .singleElement()
                .satisfies(item -> assertThat(item.isSelected()).isTrue());
    }

    @Test
    void rejectsNullOrIneligibleSelections() {
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of());
        when(handler.resolveDocumentPromotionMetadata(workflow)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.savePendingSelections(workflow, handler, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(
                        () ->
                                service.savePendingSelections(
                                        workflow,
                                        handler,
                                        List.of(
                                                WorkflowDocumentPromotionSelection.builder()
                                                        .attachmentId(99L)
                                                        .source(WorkflowMediaSource.WORKFLOW)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not eligible");
    }

    @Test
    void handlerWithoutPromotionHasEmptyPreviewAndIgnoresPendingSelections() {
        when(handler.allowsDocumentPromotion(workflow)).thenReturn(false);
        workflow.setPendingDocumentPromotions("[{\"attachmentId\":10,\"source\":\"WORKFLOW\"}]");

        assertThat(service.buildPreview(workflow, handler).getItems()).isEmpty();
        assertThat(service.resolvePromotionsForCompletion(workflow, handler)).isEmpty();
        assertThatThrownBy(
                        () ->
                                service.savePendingSelections(
                                        workflow,
                                        handler,
                                        List.of(
                                                WorkflowDocumentPromotionSelection.builder()
                                                        .attachmentId(10L)
                                                        .source(WorkflowMediaSource.WORKFLOW)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not eligible");
        verify(workflowDocumentService, never()).listWorkflowAttachments(any());
    }

    @Test
    void malformedPendingJsonFallsBackToDefaults() {
        workflow.setPendingDocumentPromotions("{not-json");
        EntityAttachmentDto attachment =
                EntityAttachmentDto.builder().id(10L).fileName("proof.pdf").build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(attachment));
        when(handler.resolveDocumentPromotionMetadata(workflow)).thenReturn(Optional.empty());

        assertThat(service.resolvePromotionsForCompletion(workflow, handler))
                .singleElement()
                .satisfies(selection -> assertThat(selection.getAttachmentId()).isEqualTo(10L));
    }

    @Test
    void promotesEligibleAttachmentOnceAndCreatesLinkedUserDocument() {
        EntityAttachmentDto dto =
                EntityAttachmentDto.builder()
                        .id(10L)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(4L)
                        .build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(10L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW)
                        .entityId(5L)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(4L)
                        .storageKey("tenants/7/workflows/5/proof.pdf")
                        .build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(dto));
        when(handler.resolveDocumentPromotionMetadata(workflow))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Nachweis Atemschutzgeräteträger")
                                        .build()));
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));
        when(userDocumentPromotionService.copyFromStorage(
                        eq(7L),
                        eq(1L),
                        eq("workflows"),
                        eq(attachment.getStorageKey()),
                        eq("proof.pdf"),
                        eq("application/pdf"),
                        eq(4L),
                        eq("Nachweis Atemschutzgeräteträger"),
                        isNull(),
                        eq(List.of()),
                        any()))
                .thenReturn(99L);
        WorkflowDocumentPromotionSelection selection =
                WorkflowDocumentPromotionSelection.builder()
                        .attachmentId(10L)
                        .source(WorkflowMediaSource.WORKFLOW)
                        .build();

        List<Long> promotedIds =
                service.promoteToUserDocuments(workflow, List.of(selection, selection), handler);

        assertThat(promotedIds).containsExactly(99L);
        verify(userDocumentPromotionService, times(1))
                .copyFromStorage(
                        eq(7L),
                        eq(1L),
                        eq("workflows"),
                        eq(attachment.getStorageKey()),
                        eq("proof.pdf"),
                        eq("application/pdf"),
                        eq(4L),
                        eq("Nachweis Atemschutzgeräteträger"),
                        isNull(),
                        eq(List.of()),
                        any());
    }

    @Test
    void promotionRejectsMissingAndForeignAttachments() {
        EntityAttachmentDto dto =
                EntityAttachmentDto.builder().id(10L).fileName("proof.pdf").build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(dto));
        when(handler.resolveDocumentPromotionMetadata(workflow)).thenReturn(Optional.empty());
        WorkflowDocumentPromotionSelection selection =
                WorkflowDocumentPromotionSelection.builder()
                        .attachmentId(10L)
                        .source(WorkflowMediaSource.WORKFLOW)
                        .build();

        when(attachmentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () -> service.promoteToUserDocuments(workflow, List.of(selection), handler))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not found");

        EntityAttachment foreign =
                EntityAttachment.builder()
                        .id(10L)
                        .entityType(EntityAttachmentType.WORKFLOW)
                        .entityId(99L)
                        .build();
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(
                        () -> service.promoteToUserDocuments(workflow, List.of(selection), handler))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void emptyPromotionSelectionIsNoOp() {
        List<Long> promotedIds = service.promoteToUserDocuments(workflow, List.of(), handler);

        assertThat(promotedIds).isEmpty();
        verify(userDocumentPromotionService, never())
                .copyFromStorage(
                        anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any(),
                        any(), any());
    }

    @Test
    void previewIncludesCommentMediaWhenHandlerDefaultsIncludeComments() {
        when(handler.resolveDocumentPromotionDefaults(workflow))
                .thenReturn(
                        WorkflowDocumentPromotionDefaults.builder()
                                .includeWorkflowMedia(false)
                                .includeCommentMedia(true)
                                .build());
        WorkflowComment comment = WorkflowComment.builder().id(30L).workflow(workflow).build();
        EntityAttachmentDto commentAttachment =
                EntityAttachmentDto.builder().id(11L).fileName("comment.png").build();
        when(commentRepository.findByWorkflow_IdOrderByIdAsc(5L)).thenReturn(List.of(comment));
        when(workflowDocumentService.listCommentAttachments(workflow, List.of(30L)))
                .thenReturn(java.util.Map.of(30L, commentAttachment));

        var preview = service.buildPreview(workflow, handler);

        assertThat(preview.getItems())
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.getAttachmentId()).isEqualTo(11L);
                            assertThat(item.getSource()).isEqualTo(WorkflowMediaSource.COMMENT);
                            assertThat(item.getCommentId()).isEqualTo(30L);
                            assertThat(item.isSelected()).isTrue();
                        });
    }

    @Test
    void previewUsesWorkflowMediaMetadataWhenPresent() {
        EntityAttachmentDto attachment =
                EntityAttachmentDto.builder().id(10L).fileName("proof.pdf").build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(attachment));
        when(handler.resolveDocumentPromotionMetadata(workflow))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title(" Official title ")
                                        .description(" Description ")
                                        .tags(List.of("tag-a"))
                                        .build()));

        var preview = service.buildPreview(workflow, handler);

        assertThat(preview.getItems())
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.getDefaultTitle()).isEqualTo("Official title");
                            assertThat(item.getDefaultDescription()).isEqualTo("Description");
                            assertThat(item.getDefaultTags()).containsExactly("tag-a");
                        });
    }

    @Test
    void resolveDefaultSelectionsReturnsPendingWhenStored() {
        workflow.setPendingDocumentPromotions(
                "[{\"attachmentId\":10,\"source\":\"WORKFLOW\",\"title\":\"Saved\"}]");

        assertThat(service.resolveDefaultSelections(workflow, handler))
                .singleElement()
                .satisfies(selection -> assertThat(selection.getTitle()).isEqualTo("Saved"));
    }

    @Test
    void resolveDefaultSelectionsFallsBackToPreviewDefaults() {
        EntityAttachmentDto attachment =
                EntityAttachmentDto.builder().id(10L).fileName("proof.pdf").build();
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of(attachment));
        when(handler.resolveDocumentPromotionMetadata(workflow)).thenReturn(Optional.empty());

        assertThat(service.resolveDefaultSelections(workflow, handler))
                .singleElement()
                .satisfies(
                        selection -> {
                            assertThat(selection.getAttachmentId()).isEqualTo(10L);
                            assertThat(selection.getSource())
                                    .isEqualTo(WorkflowMediaSource.WORKFLOW);
                            assertThat(selection.getTitle()).isEqualTo("proof.pdf");
                        });
    }

    @Test
    void promotesCommentAttachmentToUserDocuments() {
        when(handler.resolveDocumentPromotionDefaults(workflow))
                .thenReturn(
                        WorkflowDocumentPromotionDefaults.builder()
                                .includeWorkflowMedia(false)
                                .includeCommentMedia(true)
                                .build());
        WorkflowComment comment = WorkflowComment.builder().id(30L).workflow(workflow).build();
        EntityAttachmentDto dto =
                EntityAttachmentDto.builder()
                        .id(11L)
                        .fileName("comment.png")
                        .contentType("image/png")
                        .sizeBytes(8L)
                        .build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(11L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT)
                        .entityId(30L)
                        .fileName("comment.png")
                        .contentType("image/png")
                        .sizeBytes(8L)
                        .storageKey("tenants/7/workflows/5/comments/30/comment.png")
                        .build();
        when(commentRepository.findByWorkflow_IdOrderByIdAsc(5L)).thenReturn(List.of(comment));
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of());
        when(workflowDocumentService.listCommentAttachments(workflow, List.of(30L)))
                .thenReturn(java.util.Map.of(30L, dto));
        when(attachmentRepository.findById(11L)).thenReturn(Optional.of(attachment));
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));
        when(userDocumentPromotionService.copyFromStorage(
                        anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any(),
                        any(), any()))
                .thenReturn(99L);
        WorkflowDocumentPromotionSelection selection =
                WorkflowDocumentPromotionSelection.builder()
                        .attachmentId(11L)
                        .source(WorkflowMediaSource.COMMENT)
                        .build();

        service.promoteToUserDocuments(workflow, List.of(selection), handler);

        verify(userDocumentPromotionService)
                .copyFromStorage(
                        eq(7L),
                        eq(1L),
                        eq("workflows"),
                        eq(attachment.getStorageKey()),
                        eq("comment.png"),
                        eq("image/png"),
                        eq(8L),
                        eq("comment.png"),
                        isNull(),
                        eq(List.of()),
                        any());
    }

    @Test
    void promotionRejectsForeignCommentAttachment() {
        when(handler.resolveDocumentPromotionDefaults(workflow))
                .thenReturn(
                        WorkflowDocumentPromotionDefaults.builder()
                                .includeWorkflowMedia(false)
                                .includeCommentMedia(true)
                                .build());
        Workflow otherWorkflow = Workflow.builder().id(99L).tenantId(7L).build();
        WorkflowComment comment = WorkflowComment.builder().id(30L).workflow(otherWorkflow).build();
        EntityAttachmentDto dto =
                EntityAttachmentDto.builder().id(11L).fileName("comment.png").build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(11L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT)
                        .entityId(30L)
                        .build();
        when(commentRepository.findByWorkflow_IdOrderByIdAsc(5L)).thenReturn(List.of(comment));
        when(workflowDocumentService.listWorkflowAttachments(5L)).thenReturn(List.of());
        when(workflowDocumentService.listCommentAttachments(workflow, List.of(30L)))
                .thenReturn(java.util.Map.of(30L, dto));
        when(attachmentRepository.findById(11L)).thenReturn(Optional.of(attachment));
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));
        WorkflowDocumentPromotionSelection selection =
                WorkflowDocumentPromotionSelection.builder()
                        .attachmentId(11L)
                        .source(WorkflowMediaSource.COMMENT)
                        .build();

        assertThatThrownBy(
                        () -> service.promoteToUserDocuments(workflow, List.of(selection), handler))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("does not belong");
    }
}
