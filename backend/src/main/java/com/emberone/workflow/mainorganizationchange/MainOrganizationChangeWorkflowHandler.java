/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.mainorganizationchange;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.MainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import com.emberone.workflow.spi.WorkflowOpenExclusivityKeys;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class MainOrganizationChangeWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<MainOrganizationChangeWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.mainOrganizationChange";

    private final WorkflowDocumentService workflowDocumentService;
    private final UserOrganizationMembershipService membershipService;
    private final OrganizationRepository organizationRepository;
    private final WorkflowTypeMappingService workflowTypeMappingService;

    public MainOrganizationChangeWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            UserOrganizationMembershipService membershipService,
            OrganizationRepository organizationRepository,
            WorkflowTypeMappingService workflowTypeMappingService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.membershipService = membershipService;
        this.organizationRepository = organizationRepository;
        this.workflowTypeMappingService = workflowTypeMappingService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.MAIN_ORGANIZATION_CHANGE;
    }

    @Override
    public WorkflowMode mode() {
        return WorkflowMode.DYNAMIC;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx, Workflow existing) {
        MainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getNewMainOrganizationId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenConcurrencyKeys.organization(payload.getNewMainOrganizationId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open main organization change request for this organization";
    }

    @Override
    public List<String> resolveOpenExclusivityKeys(
            WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx, Workflow existing) {
        if (ctx.getOpenedByUserId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenExclusivityKeys.userMainOrganizationChange(ctx.getOpenedByUserId()));
    }

    @Override
    public String openExclusivityConflictCode() {
        return WorkflowValidationException.CODE_MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN;
    }

    @Override
    public String openExclusivityConflictMessage() {
        return "A main organization change is already in progress for this member";
    }

    @Override
    public boolean requiresOpenerOrganizationMembership() {
        return false;
    }

    @Override
    public Class<MainOrganizationChangeWorkflowPayload> payloadType() {
        return MainOrganizationChangeWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - main organization change is not possible";
    }

    /**
     * With a current main org that org is the context approver (plus the new main via dynamic
     * orgs). Without a current main org the new main is the only context, so only that org needs to
     * approve.
     */
    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx) {
        return findMainOrganization(ctx.getOpenedByUserId())
                .orElseGet(
                        () -> {
                            MainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
                            if (payload != null && payload.getNewMainOrganizationId() != null) {
                                return payload.getNewMainOrganizationId();
                            }
                            throw new WorkflowValidationException(missingMembershipMessage());
                        });
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return findMembershipOrganization(openedByUserId)
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        return findMainOrganization(workflow.getOpenedByUserId())
                .orElseGet(
                        () -> {
                            MainOrganizationChangeWorkflowPayload payload =
                                    readPayloadIfPresent(
                                            workflow, MainOrganizationChangeWorkflowPayload.class);
                            if (payload != null && payload.getNewMainOrganizationId() != null) {
                                return payload.getNewMainOrganizationId();
                            }
                            return resolveSecurityContextOrganization(
                                    workflow.getOpenedByUserId(), workflow.getTenantId());
                        });
    }

    @Override
    public List<ApproverOrg> resolveDynamicOrganizations(
            WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx) {
        MainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getNewMainOrganizationId() == null) {
            return List.of();
        }
        return List.of(new ApproverOrg(payload.getNewMainOrganizationId()));
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        MainOrganizationChangeWorkflowPayload payload =
                readPayloadIfPresent(workflow, MainOrganizationChangeWorkflowPayload.class);
        String name = payload != null ? payload.getNewMainOrganizationName() : null;
        return resolveLocalizedDocumentPromotionMetadata(workflow, DOCUMENT_PROMOTION_PREFIX, name);
    }

    @Override
    public void validateOpen(WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx) {
        validatePayload(ctx, null);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx, Workflow existing) {
        validatePayload(ctx, existing);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachMainOrganizationChangeAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachMainOrganizationChangeAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        MainOrganizationChangeWorkflowPayload payload =
                readPayload(workflow, MainOrganizationChangeWorkflowPayload.class);
        membershipService.changeMainOrganization(
                workflow.getOpenedByUserId(), payload.getNewMainOrganizationId());
        log.info(
                "Changed main organization for user {} to organization {} via workflow {}",
                workflow.getOpenedByUserId(),
                payload.getNewMainOrganizationId(),
                workflow.getId());
    }

    private void validatePayload(
            WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx, Workflow existing) {
        MainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);

        Long newMainOrganizationId = payload.getNewMainOrganizationId();
        if (newMainOrganizationId == null) {
            throw new WorkflowValidationException("New main organization is required");
        }

        Long currentMainOrgId = findMainOrganization(ctx.getOpenedByUserId()).orElse(null);
        if (currentMainOrgId != null && newMainOrganizationId.equals(currentMainOrgId)) {
            throw new WorkflowValidationException(
                    "New main organization must differ from your current main organization");
        }

        Organization newMainOrganization =
                organizationRepository
                        .findById(newMainOrganizationId)
                        .orElseThrow(
                                () -> new OrganizationNotFoundException(newMainOrganizationId));
        if (!newMainOrganization.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException(
                    "New main organization does not belong to your tenant");
        }

        if (!membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                ctx.getOpenedByUserId(), newMainOrganizationId, Instant.now())) {
            throw new WorkflowValidationException(
                    "You must be an active member of the new main organization");
        }

        workflowTypeMappingService.requireMapping(
                newMainOrganizationId, WorkflowType.MAIN_ORGANIZATION_CHANGE.getId());

        String reason = trimToNull(payload.getReason());
        if (reason != null
                && reason.length() > MainOrganizationChangeWorkflowPayload.MAX_REASON_LENGTH) {
            throw new WorkflowValidationException(
                    "Reason must be at most "
                            + MainOrganizationChangeWorkflowPayload.MAX_REASON_LENGTH
                            + " characters");
        }

        payload.setNewMainOrganizationId(newMainOrganizationId);
        payload.setNewMainOrganizationName(newMainOrganization.getName());
        payload.setReason(reason);

        Long contextOrgId = resolveContextOrganization(ctx);
        if (existing == null) {
            validateWorkflowDraftAttachments(
                    ctx.getOpenedByUserId(),
                    contextOrgId,
                    payload.getAttachmentIds(),
                    MainOrganizationChangeWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        } else {
            validateWorkflowAttachmentsForUpdate(
                    ctx.getOpenedByUserId(),
                    contextOrgId,
                    existing.getId(),
                    payload.getAttachmentIds(),
                    MainOrganizationChangeWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
