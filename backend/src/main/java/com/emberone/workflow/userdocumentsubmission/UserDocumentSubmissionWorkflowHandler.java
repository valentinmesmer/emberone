/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.userdocumentsubmission;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.UserDocumentSubmissionWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class UserDocumentSubmissionWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<UserDocumentSubmissionWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.userDocumentSubmission";

    private final WorkflowDocumentService workflowDocumentService;

    public UserDocumentSubmissionWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.USER_DOCUMENT_SUBMISSION;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    @Override
    public Class<UserDocumentSubmissionWorkflowPayload> payloadType() {
        return UserDocumentSubmissionWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - document submission is not possible";
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        UserDocumentSubmissionWorkflowPayload payload =
                readPayloadIfPresent(workflow, UserDocumentSubmissionWorkflowPayload.class);
        if (payload == null) {
            return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                    workflow, DOCUMENT_PROMOTION_PREFIX);
        }
        String title = trim(payload.getTitle());
        if (!StringUtils.hasText(title)) {
            return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                    workflow, DOCUMENT_PROMOTION_PREFIX);
        }
        return Optional.of(
                WorkflowDocumentPromotionMetadata.builder()
                        .title(title)
                        .description(
                                StringUtils.hasText(payload.getDescription())
                                        ? payload.getDescription().trim()
                                        : null)
                        .tags(normalizeTags(payload.getTags()))
                        .build());
    }

    @Override
    public void validateOpen(WorkflowOpenContext<UserDocumentSubmissionWorkflowPayload> ctx) {
        UserDocumentSubmissionWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        normalizePayload(payload);

        Long contextOrgId = resolveContextOrganization(ctx);
        validateWorkflowDraftAttachments(
                ctx.getOpenedByUserId(),
                contextOrgId,
                payload.getAttachmentIds(),
                UserDocumentSubmissionWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<UserDocumentSubmissionWorkflowPayload> ctx, Workflow existing) {
        UserDocumentSubmissionWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        normalizePayload(payload);

        Long contextOrgId = resolveContextOrganization(ctx);
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                contextOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                UserDocumentSubmissionWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachUserDocumentSubmissionAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachUserDocumentSubmissionAttachmentsToWorkflow(workflow);
    }

    private void normalizePayload(UserDocumentSubmissionWorkflowPayload payload) {
        String title = trim(payload.getTitle());
        if (!StringUtils.hasText(title)) {
            throw new WorkflowValidationException("Title must not be empty");
        }
        payload.setTitle(title);
        if (payload.getDescription() != null) {
            String description = payload.getDescription().trim();
            payload.setDescription(description.isEmpty() ? null : description);
        }
        payload.setTags(normalizeTags(payload.getTags()));
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.length() > 128) {
                throw new WorkflowValidationException("Tag must be at most 128 characters");
            }
            normalized.add(trimmed);
        }
        return new ArrayList<>(normalized);
    }
}
