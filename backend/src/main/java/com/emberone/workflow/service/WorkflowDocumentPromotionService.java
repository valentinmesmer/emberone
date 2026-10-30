/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.userdocument.dto.UserDocumentLinkInput;
import com.emberone.userdocument.service.UserDocumentPromotionService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowComment;
import com.emberone.workflow.domain.WorkflowMediaSource;
import com.emberone.workflow.dto.*;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowCommentRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDocumentPromotionService {

    private static final TypeReference<List<WorkflowDocumentPromotionSelection>>
            SELECTION_LIST_TYPE = new TypeReference<>() {};

    private final EntityAttachmentRepository entityAttachmentRepository;
    private final WorkflowCommentRepository workflowCommentRepository;
    private final WorkflowDocumentService workflowDocumentService;
    private final UserDocumentPromotionService userDocumentPromotionService;
    private final JsonMapper jsonMapper;

    @Value("${object-storage.workflows-bucket}")
    private String workflowsBucket;

    private static String promotionKey(Long attachmentId, WorkflowMediaSource source) {
        return source.name() + ":" + attachmentId;
    }

    @Transactional(readOnly = true)
    public WorkflowDocumentPromotionPreviewDto buildPreview(
            Workflow workflow, WorkflowHandler<?> handler) {
        if (!handler.allowsDocumentPromotion(workflow)) {
            return WorkflowDocumentPromotionPreviewDto.builder().build();
        }
        WorkflowDocumentPromotionDefaults defaults =
                handler.resolveDocumentPromotionDefaults(workflow);
        Set<String> selectedKeys = resolveSelectedKeys(workflow, handler, defaults);
        List<WorkflowDocumentPromotionItemDto> items = new ArrayList<>();

        List<EntityAttachmentDto> workflowAttachments =
                workflowDocumentService.listWorkflowAttachments(workflow.getId());
        Optional<WorkflowDocumentPromotionMetadata> workflowMediaMetadata =
                handler.resolveDocumentPromotionMetadata(workflow);
        for (EntityAttachmentDto attachment : workflowAttachments) {
            WorkflowDocumentPromotionMetadata metadata = workflowMediaMetadata.orElse(null);
            String defaultTitle =
                    metadata != null && StringUtils.hasText(metadata.getTitle())
                            ? metadata.getTitle().trim()
                            : attachment.getFileName();
            String defaultDescription =
                    metadata != null && StringUtils.hasText(metadata.getDescription())
                            ? metadata.getDescription().trim()
                            : null;
            List<String> defaultTags =
                    metadata != null && metadata.getTags() != null ? metadata.getTags() : List.of();

            String key = promotionKey(attachment.getId(), WorkflowMediaSource.WORKFLOW);
            items.add(
                    WorkflowDocumentPromotionItemDto.builder()
                            .attachmentId(attachment.getId())
                            .source(WorkflowMediaSource.WORKFLOW)
                            .attachment(attachment)
                            .selected(selectedKeys.contains(key))
                            .defaultTitle(defaultTitle)
                            .defaultDescription(defaultDescription)
                            .defaultTags(defaultTags)
                            .build());
        }

        List<WorkflowComment> comments =
                workflowCommentRepository.findByWorkflow_IdOrderByIdAsc(workflow.getId());
        List<Long> commentIds = comments.stream().map(WorkflowComment::getId).toList();
        Map<Long, EntityAttachmentDto> commentAttachments =
                workflowDocumentService.listCommentAttachments(workflow, commentIds);

        for (WorkflowComment comment : comments) {
            EntityAttachmentDto attachment = commentAttachments.get(comment.getId());
            if (attachment == null) {
                continue;
            }
            String key = promotionKey(attachment.getId(), WorkflowMediaSource.COMMENT);
            items.add(
                    WorkflowDocumentPromotionItemDto.builder()
                            .attachmentId(attachment.getId())
                            .source(WorkflowMediaSource.COMMENT)
                            .commentId(comment.getId())
                            .attachment(attachment)
                            .selected(selectedKeys.contains(key))
                            .defaultTitle(attachment.getFileName())
                            .defaultTags(List.of())
                            .build());
        }

        return WorkflowDocumentPromotionPreviewDto.builder().items(items).build();
    }

    @Transactional(readOnly = true)
    public List<WorkflowDocumentPromotionSelection> resolveDefaultSelections(
            Workflow workflow, WorkflowHandler<?> handler) {
        WorkflowDocumentPromotionDefaults defaults =
                handler.resolveDocumentPromotionDefaults(workflow);
        List<WorkflowDocumentPromotionSelection> pending = readPendingSelections(workflow);
        if (pending != null) {
            return pending;
        }
        return buildPreview(workflow, handler).getItems().stream()
                .filter(
                        item ->
                                resolveSelectedKeys(workflow, handler, defaults)
                                        .contains(
                                                promotionKey(
                                                        item.getAttachmentId(), item.getSource())))
                .map(this::toSelection)
                .toList();
    }

    @Transactional
    public WorkflowDocumentPromotionPreviewDto savePendingSelections(
            Workflow workflow,
            WorkflowHandler<?> handler,
            List<WorkflowDocumentPromotionSelection> selections) {
        validateSelections(workflow, handler, selections);
        WorkflowDocumentPromotionPreviewDto preview = buildPreview(workflow, handler);
        workflow.setPendingDocumentPromotions(
                serializeSelections(enrichSelections(preview, selections)));
        return buildPreview(workflow, handler);
    }

    @Transactional(readOnly = true)
    public List<WorkflowDocumentPromotionSelection> resolvePromotionsForCompletion(
            Workflow workflow, WorkflowHandler<?> handler) {
        if (!handler.allowsDocumentPromotion(workflow)) {
            return List.of();
        }
        List<WorkflowDocumentPromotionSelection> pending = readPendingSelections(workflow);
        if (pending != null) {
            return pending;
        }
        return resolveDefaultSelections(workflow, handler);
    }

    @Transactional
    public List<Long> promoteToUserDocuments(
            Workflow workflow,
            List<WorkflowDocumentPromotionSelection> selections,
            WorkflowHandler<?> handler) {
        if (selections == null || selections.isEmpty()) {
            return List.of();
        }

        WorkflowDocumentPromotionPreviewDto preview = buildPreview(workflow, handler);
        Map<String, WorkflowDocumentPromotionItemDto> previewByKey = new HashMap<>();
        for (WorkflowDocumentPromotionItemDto item : preview.getItems()) {
            previewByKey.put(promotionKey(item.getAttachmentId(), item.getSource()), item);
        }

        Set<String> seen = new LinkedHashSet<>();
        List<Long> promotedDocumentIds = new ArrayList<>();
        for (WorkflowDocumentPromotionSelection selection : selections) {
            String key = promotionKey(selection.getAttachmentId(), selection.getSource());
            if (!seen.add(key)) {
                continue;
            }
            WorkflowDocumentPromotionItemDto previewItem = previewByKey.get(key);
            if (previewItem == null) {
                throw new WorkflowValidationException(
                        "Attachment is not eligible for document promotion");
            }
            promotedDocumentIds.add(promoteSingle(workflow, previewItem));
        }
        return promotedDocumentIds;
    }

    private Set<String> resolveSelectedKeys(
            Workflow workflow,
            WorkflowHandler<?> handler,
            WorkflowDocumentPromotionDefaults defaults) {
        List<WorkflowDocumentPromotionSelection> pending = readPendingSelections(workflow);
        if (pending != null) {
            return pending.stream()
                    .map(
                            selection ->
                                    promotionKey(
                                            selection.getAttachmentId(), selection.getSource()))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        Set<String> keys = new LinkedHashSet<>();
        if (defaults.isIncludeWorkflowMedia()) {
            for (EntityAttachmentDto attachment :
                    workflowDocumentService.listWorkflowAttachments(workflow.getId())) {
                keys.add(promotionKey(attachment.getId(), WorkflowMediaSource.WORKFLOW));
            }
        }
        if (defaults.isIncludeCommentMedia()) {
            List<WorkflowComment> comments =
                    workflowCommentRepository.findByWorkflow_IdOrderByIdAsc(workflow.getId());
            List<Long> commentIds = comments.stream().map(WorkflowComment::getId).toList();
            Map<Long, EntityAttachmentDto> commentAttachments =
                    workflowDocumentService.listCommentAttachments(workflow, commentIds);
            for (WorkflowComment comment : comments) {
                EntityAttachmentDto attachment = commentAttachments.get(comment.getId());
                if (attachment != null) {
                    keys.add(promotionKey(attachment.getId(), WorkflowMediaSource.COMMENT));
                }
            }
        }
        return keys;
    }

    private void validateSelections(
            Workflow workflow,
            WorkflowHandler<?> handler,
            List<WorkflowDocumentPromotionSelection> selections) {
        if (selections == null) {
            throw new WorkflowValidationException("Document promotion selections are required");
        }
        WorkflowDocumentPromotionPreviewDto preview = buildPreview(workflow, handler);
        Map<String, WorkflowDocumentPromotionItemDto> previewByKey = new HashMap<>();
        for (WorkflowDocumentPromotionItemDto item : preview.getItems()) {
            previewByKey.put(promotionKey(item.getAttachmentId(), item.getSource()), item);
        }
        Set<String> seen = new LinkedHashSet<>();
        for (WorkflowDocumentPromotionSelection selection : selections) {
            String key = promotionKey(selection.getAttachmentId(), selection.getSource());
            if (!seen.add(key)) {
                continue;
            }
            if (!previewByKey.containsKey(key)) {
                throw new WorkflowValidationException(
                        "Attachment is not eligible for document promotion");
            }
        }
    }

    List<WorkflowDocumentPromotionSelection> readPendingSelections(Workflow workflow) {
        if (!StringUtils.hasText(workflow.getPendingDocumentPromotions())) {
            return null;
        }
        return readSelections(workflow.getPendingDocumentPromotions());
    }

    private List<WorkflowDocumentPromotionSelection> readSelections(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            List<WorkflowDocumentPromotionSelection> selections =
                    jsonMapper.readValue(json, SELECTION_LIST_TYPE);
            return selections == null ? null : List.copyOf(selections);
        } catch (JacksonException e) {
            log.warn("Failed to parse workflow document promotion selections", e);
            return null;
        }
    }

    private String serializeSelections(List<WorkflowDocumentPromotionSelection> selections) {
        if (selections == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(selections);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid document promotion selection payload");
        }
    }

    private WorkflowDocumentPromotionSelection toSelection(WorkflowDocumentPromotionItemDto item) {
        return enrichSelection(
                item,
                WorkflowDocumentPromotionSelection.builder()
                        .attachmentId(item.getAttachmentId())
                        .source(item.getSource())
                        .build());
    }

    private List<WorkflowDocumentPromotionSelection> enrichSelections(
            WorkflowDocumentPromotionPreviewDto preview,
            List<WorkflowDocumentPromotionSelection> selections) {
        Map<String, WorkflowDocumentPromotionItemDto> previewByKey = new HashMap<>();
        for (WorkflowDocumentPromotionItemDto item : preview.getItems()) {
            previewByKey.put(promotionKey(item.getAttachmentId(), item.getSource()), item);
        }

        Set<String> seen = new LinkedHashSet<>();
        List<WorkflowDocumentPromotionSelection> enriched = new ArrayList<>();
        for (WorkflowDocumentPromotionSelection selection : selections) {
            String key = promotionKey(selection.getAttachmentId(), selection.getSource());
            if (!seen.add(key)) {
                continue;
            }
            WorkflowDocumentPromotionItemDto previewItem = previewByKey.get(key);
            if (previewItem == null) {
                throw new WorkflowValidationException(
                        "Attachment is not eligible for document promotion");
            }
            enriched.add(enrichSelection(previewItem, selection));
        }
        return enriched;
    }

    private WorkflowDocumentPromotionSelection enrichSelection(
            WorkflowDocumentPromotionItemDto previewItem,
            WorkflowDocumentPromotionSelection selection) {
        return WorkflowDocumentPromotionSelection.builder()
                .attachmentId(selection.getAttachmentId())
                .source(selection.getSource())
                .title(previewItem.getDefaultTitle())
                .description(previewItem.getDefaultDescription())
                .tags(
                        previewItem.getDefaultTags() != null
                                ? previewItem.getDefaultTags()
                                : List.of())
                .build();
    }

    private Long promoteSingle(Workflow workflow, WorkflowDocumentPromotionItemDto previewItem) {
        EntityAttachment attachment = resolveAttachment(workflow, previewItem);
        String title = previewItem.getDefaultTitle();
        String description = previewItem.getDefaultDescription();
        List<String> tags =
                previewItem.getDefaultTags() != null ? previewItem.getDefaultTags() : List.of();

        Long documentId =
                userDocumentPromotionService.copyFromStorage(
                        workflow.getTenantId(),
                        workflow.getOpenedByUserId(),
                        workflowsBucket,
                        attachment.getStorageKey(),
                        attachment.getFileName(),
                        attachment.getContentType(),
                        attachment.getSizeBytes(),
                        title,
                        description,
                        tags,
                        List.of(
                                UserDocumentLinkInput.builder()
                                        .linkType(AppLinkType.WORKFLOW)
                                        .linkId(workflow.getId())
                                        .build()));

        log.info(
                "Promoted workflow {} attachment {} to user document for user {}",
                workflow.getId(),
                attachment.getId(),
                workflow.getOpenedByUserId());
        return documentId;
    }

    private EntityAttachment resolveAttachment(
            Workflow workflow, WorkflowDocumentPromotionItemDto previewItem) {
        EntityAttachment attachment =
                entityAttachmentRepository
                        .findById(previewItem.getAttachmentId())
                        .orElseThrow(() -> new WorkflowValidationException("Attachment not found"));

        if (previewItem.getSource() == WorkflowMediaSource.WORKFLOW) {
            if (attachment.getEntityType() != EntityAttachmentType.WORKFLOW
                    || !attachment.getEntityId().equals(workflow.getId())) {
                throw new WorkflowValidationException(
                        "Attachment does not belong to this workflow");
            }
            return attachment;
        }

        if (previewItem.getSource() == WorkflowMediaSource.COMMENT) {
            if (attachment.getEntityType() != EntityAttachmentType.WORKFLOW_COMMENT) {
                throw new WorkflowValidationException(
                        "Attachment is not a workflow comment attachment");
            }
            WorkflowComment comment =
                    workflowCommentRepository
                            .findById(attachment.getEntityId())
                            .orElseThrow(
                                    () -> new WorkflowValidationException("Comment not found"));
            if (!comment.getWorkflow().getId().equals(workflow.getId())) {
                throw new WorkflowValidationException(
                        "Comment attachment does not belong to this workflow");
            }
            return attachment;
        }

        throw new WorkflowValidationException("Unsupported media source");
    }
}
