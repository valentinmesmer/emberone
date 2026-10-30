/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.dto.WorkflowDocumentPromotionDefaults;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared self-service workflow logic: membership-based context org, draft attachment checks,
 * payload parsing.
 */
public abstract class AbstractMembershipContextWorkflowHandler<P> implements WorkflowHandler<P> {

    protected final UserRepository userRepository;
    protected final UserOrganizationMembershipRepository membershipRepository;
    protected final EntityAttachmentRepository entityAttachmentRepository;
    protected final JsonMapper jsonMapper;
    protected final LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    protected AbstractMembershipContextWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            JsonMapper jsonMapper) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.entityAttachmentRepository = entityAttachmentRepository;
        this.documentPromotionMetadataSupport = documentPromotionMetadataSupport;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public WorkflowMode mode() {
        return WorkflowMode.STATIC;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return false;
    }

    @Override
    public WorkflowDocumentPromotionDefaults resolveDocumentPromotionDefaults(Workflow workflow) {
        return WorkflowDocumentPromotionDefaults.builder()
                .includeWorkflowMedia(true)
                .includeCommentMedia(false)
                .build();
    }

    @Override
    public Long resolveContextOrganization(WorkflowOpenContext<P> ctx) {
        return requireMembershipOrganization(ctx.getOpenedByUserId());
    }

    /**
     * Organization from the opener's active main membership. Used for draft uploads and pre-open
     * security checks. Handlers that override {@link #requiresOpenerOrganizationMembership()} to
     * {@code false} must not call this without a fallback.
     */
    protected Long requireMembershipOrganization(Long userId) {
        return findMainOrganization(userId)
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    protected Optional<Long> findMainOrganization(Long userId) {
        return membershipRepository
                .findActiveByUser_IdAndMainTrue(userId, Instant.now())
                .map(UserOrganizationMembership::getOrganization)
                .map(Organization::getId);
    }

    /** Main organization if present, otherwise the first active membership. */
    protected Optional<Long> findMembershipOrganization(Long userId) {
        return findMainOrganization(userId)
                .or(
                        () ->
                                membershipRepository
                                        .findFirstActiveByUser_IdOrderByIdAsc(userId, Instant.now())
                                        .map(UserOrganizationMembership::getOrganization)
                                        .map(Organization::getId));
    }

    protected abstract String missingMembershipMessage();

    protected void requirePayloadPresent(P payload) {
        if (payload == null) {
            throw new WorkflowValidationException("Payload is required");
        }
    }

    protected User requireOpeningUser(WorkflowOpenContext<P> ctx) {
        User user =
                userRepository
                        .findById(ctx.getOpenedByUserId())
                        .orElseThrow(() -> new WorkflowValidationException("User not found"));
        if (!user.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException("Tenant does not match the user");
        }
        return user;
    }

    protected void validateWorkflowDraftAttachments(
            Long userId,
            Long organizationId,
            List<Long> attachmentIds,
            int maxAttachments,
            boolean required) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            if (required) {
                throw new WorkflowValidationException("At least one attachment is required");
            }
            return;
        }
        if (attachmentIds.size() > maxAttachments) {
            throw new WorkflowValidationException(
                    "At most " + maxAttachments + " attachments are allowed");
        }
        Set<Long> unique = new LinkedHashSet<>(attachmentIds);
        if (unique.size() != attachmentIds.size()) {
            throw new WorkflowValidationException("Duplicate attachment ids");
        }
        for (Long attachmentId : unique) {
            validateWorkflowDraftAttachment(userId, organizationId, attachmentId);
        }
    }

    /**
     * For payload updates: each id must be a draft owned by the opener, or already linked to this
     * workflow.
     */
    protected void validateWorkflowAttachmentsForUpdate(
            Long userId,
            Long organizationId,
            Long workflowId,
            List<Long> attachmentIds,
            int maxAttachments,
            boolean required) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            if (required) {
                throw new WorkflowValidationException("At least one attachment is required");
            }
            return;
        }
        if (attachmentIds.size() > maxAttachments) {
            throw new WorkflowValidationException(
                    "At most " + maxAttachments + " attachments are allowed");
        }
        Set<Long> unique = new LinkedHashSet<>(attachmentIds);
        if (unique.size() != attachmentIds.size()) {
            throw new WorkflowValidationException("Duplicate attachment ids");
        }
        for (Long attachmentId : unique) {
            EntityAttachment attachment =
                    entityAttachmentRepository
                            .findById(attachmentId)
                            .orElseThrow(
                                    () ->
                                            new WorkflowValidationException(
                                                    "Attachment not found or does not belong to your organization / account"));
            if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW
                    && workflowId.equals(attachment.getEntityId())) {
                continue;
            }
            if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW_DRAFT
                    && userId.equals(attachment.getEntityId())
                    && userId.equals(attachment.getUploadedBy())
                    && (organizationId == null
                            || organizationId.equals(attachment.getOrganizationId()))) {
                continue;
            }
            throw new WorkflowValidationException(
                    "Attachment not found or does not belong to your organization / account");
        }
    }

    protected P readPayload(Workflow workflow, Class<P> payloadType) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(workflow.getPayload(), payloadType);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    protected P readPayloadIfPresent(Workflow workflow, Class<P> payloadType) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(workflow.getPayload(), payloadType);
        } catch (JacksonException e) {
            return null;
        }
    }

    protected Optional<WorkflowDocumentPromotionMetadata> resolveLocalizedDocumentPromotionMetadata(
            Workflow workflow, String messagePrefix) {
        return documentPromotionMetadataSupport.fromMessagePrefix(workflow, messagePrefix);
    }

    /**
     * Payload-driven promotion titles: interpolate {@code {name}} when present, otherwise use the
     * {@code titleFallback} message for the prefix.
     */
    protected Optional<WorkflowDocumentPromotionMetadata> resolveLocalizedDocumentPromotionMetadata(
            Workflow workflow, String messagePrefix, String nameParam) {
        if (StringUtils.hasText(nameParam)) {
            return documentPromotionMetadataSupport.fromInterpolatedTitle(
                    workflow, messagePrefix, Map.of("name", nameParam.trim()));
        }
        return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                workflow, messagePrefix);
    }

    private void validateWorkflowDraftAttachment(
            Long userId, Long organizationId, Long attachmentId) {
        if (organizationId == null) {
            EntityAttachment attachment =
                    entityAttachmentRepository
                            .findById(attachmentId)
                            .orElseThrow(
                                    () ->
                                            new WorkflowValidationException(
                                                    "Attachment not found or does not belong to your organization / account"));
            if (attachment.getEntityType() != EntityAttachmentType.WORKFLOW_DRAFT
                    || !userId.equals(attachment.getEntityId())
                    || !userId.equals(attachment.getUploadedBy())) {
                throw new WorkflowValidationException(
                        "Attachment not found or does not belong to your organization / account");
            }
            return;
        }
        EntityAttachment attachment =
                entityAttachmentRepository
                        .findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                                attachmentId,
                                EntityAttachmentType.WORKFLOW_DRAFT,
                                userId,
                                organizationId)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Attachment not found or does not belong to your organization / account"));

        if (!attachment.getUploadedBy().equals(userId)) {
            throw new WorkflowValidationException("Attachment was not uploaded by you");
        }
    }
}
