/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationjoinrequest;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationJoinRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class OrganizationJoinRequestWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<OrganizationJoinRequestWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.organizationJoinRequest";

    private final WorkflowDocumentService workflowDocumentService;
    private final OnboardingInvitationService onboardingInvitationService;
    private final UserService userService;
    private final OrganizationRepository organizationRepository;
    private final WorkflowTypeMappingService workflowTypeMappingService;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public OrganizationJoinRequestWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            OnboardingInvitationService onboardingInvitationService,
            UserService userService,
            OrganizationRepository organizationRepository,
            WorkflowTypeMappingService workflowTypeMappingService,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.onboardingInvitationService = onboardingInvitationService;
        this.userService = userService;
        this.organizationRepository = organizationRepository;
        this.workflowTypeMappingService = workflowTypeMappingService;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ORGANIZATION_JOIN_REQUEST;
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
    public boolean requiresOpenerOrganizationMembership() {
        return false;
    }

    @Override
    public boolean mayOpenWithoutActiveOrganizationMembership() {
        return true;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx, Workflow existing) {
        OrganizationJoinRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getTargetOrganizationId() == null) {
            return List.of();
        }
        return List.of(WorkflowOpenConcurrencyKeys.organization(payload.getTargetOrganizationId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_ORGANIZATION_JOIN_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open join request for this organization";
    }

    @Override
    public Class<OrganizationJoinRequestWorkflowPayload> payloadType() {
        return OrganizationJoinRequestWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - organization join request is not possible";
    }

    /**
     * With an active membership the main (or first) org is the context approver; without one the
     * target org is the only context, so only that org needs to approve.
     */
    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx) {
        return findMembershipOrganization(ctx.getOpenedByUserId())
                .orElseGet(
                        () -> {
                            OrganizationJoinRequestWorkflowPayload payload = ctx.getPayload();
                            if (payload != null && payload.getTargetOrganizationId() != null) {
                                return payload.getTargetOrganizationId();
                            }
                            throw new WorkflowValidationException(missingMembershipMessage());
                        });
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return findMembershipOrganization(openedByUserId)
                .orElseGet(() -> findFirstEnabledJoinRequestOrganization());
    }

    /**
     * Target org from the payload - not the opener's current membership. Membership can differ from
     * the join target (e.g. main org elsewhere), and org-scoped lists / {@code WORKFLOW_READ} must
     * key off the organization the request is about.
     */
    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        OrganizationJoinRequestWorkflowPayload payload =
                readPayloadIfPresent(workflow, OrganizationJoinRequestWorkflowPayload.class);
        if (payload != null && payload.getTargetOrganizationId() != null) {
            return payload.getTargetOrganizationId();
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public List<ApproverOrg> resolveDynamicOrganizations(
            WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx) {
        OrganizationJoinRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getTargetOrganizationId() == null) {
            return List.of();
        }
        return List.of(new ApproverOrg(payload.getTargetOrganizationId()));
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        OrganizationJoinRequestWorkflowPayload payload =
                readPayloadIfPresent(workflow, OrganizationJoinRequestWorkflowPayload.class);
        String name = payload != null ? payload.getTargetOrganizationName() : null;
        return resolveLocalizedDocumentPromotionMetadata(workflow, DOCUMENT_PROMOTION_PREFIX, name);
    }

    @Override
    public void validateOpen(WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx) {
        validatePayload(ctx, null);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx, Workflow existing) {
        validatePayload(ctx, existing);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachOrganizationJoinRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachOrganizationJoinRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        OrganizationJoinRequestWorkflowPayload payload =
                readPayload(workflow, OrganizationJoinRequestWorkflowPayload.class);
        Long targetOrganizationId = payload.getTargetOrganizationId();
        if (!onboardingInvitationService.recordInvitationForAdmission(
                workflow.getTenantId(), workflow.getOpenedByUserId(), targetOrganizationId, null)) {
            log.info(
                    "Opened onboarding for user {} in organization {} via workflow {}",
                    workflow.getOpenedByUserId(),
                    targetOrganizationId,
                    workflow.getId());
            return;
        }
        userService.ensureOrganizationMembershipAndAllMembersGroup(
                workflow.getTenantId(), workflow.getOpenedByUserId(), targetOrganizationId);
        log.info(
                "Directly admitted user {} to organization {} via workflow {} (onboarding already fulfilled)",
                workflow.getOpenedByUserId(),
                targetOrganizationId,
                workflow.getId());
    }

    private void validatePayload(
            WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx, Workflow existing) {
        OrganizationJoinRequestWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);

        Long targetOrganizationId = payload.getTargetOrganizationId();
        if (targetOrganizationId == null) {
            throw new WorkflowValidationException("Target organization is required");
        }

        if (existing != null) {
            // Approver slots are resolved once at open; a different target needs its own workflow.
            OrganizationJoinRequestWorkflowPayload previous =
                    readPayloadIfPresent(existing, OrganizationJoinRequestWorkflowPayload.class);
            if (previous != null
                    && previous.getTargetOrganizationId() != null
                    && !previous.getTargetOrganizationId().equals(targetOrganizationId)) {
                throw new WorkflowValidationException(
                        "Target organization cannot be changed; cancel and open a new request");
            }
        }

        Long membershipOrgId = findMembershipOrganization(ctx.getOpenedByUserId()).orElse(null);
        if (membershipOrgId != null && targetOrganizationId.equals(membershipOrgId)) {
            throw new WorkflowValidationException(
                    "Target organization must differ from your main organization");
        }

        Organization targetOrganization =
                organizationRepository
                        .findById(targetOrganizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(targetOrganizationId));
        if (!targetOrganization.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException(
                    "Target organization does not belong to your tenant");
        }

        if (membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                ctx.getOpenedByUserId(), targetOrganizationId, Instant.now())) {
            throw new WorkflowValidationException(
                    "You are already an active member of the target organization");
        }

        if (onboardingInvitationService.hasPendingForUserAndOrganization(
                ctx.getOpenedByUserId(), targetOrganizationId)) {
            throw new WorkflowValidationException(
                    "You already have onboarding for this organization");
        }

        workflowTypeMappingService.requireMapping(
                targetOrganizationId, WorkflowType.ORGANIZATION_JOIN_REQUEST.getId());

        String reason = trimToNull(payload.getReason());
        if (reason != null
                && reason.length() > OrganizationJoinRequestWorkflowPayload.MAX_REASON_LENGTH) {
            throw new WorkflowValidationException(
                    "Reason must be at most "
                            + OrganizationJoinRequestWorkflowPayload.MAX_REASON_LENGTH
                            + " characters");
        }

        payload.setTargetOrganizationId(targetOrganizationId);
        payload.setTargetOrganizationName(targetOrganization.getName());
        payload.setReason(reason);

        if (existing == null) {
            validateWorkflowDraftAttachments(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    payload.getAttachmentIds(),
                    OrganizationJoinRequestWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        } else {
            validateWorkflowAttachmentsForUpdate(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    existing.getId(),
                    payload.getAttachmentIds(),
                    OrganizationJoinRequestWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        }
    }

    private Long findFirstEnabledJoinRequestOrganization() {
        return workflowDefinitionRepository
                .findByWorkflowTypeId(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId())
                .stream()
                .filter(WorkflowDefinition::isEnabled)
                .map(WorkflowDefinition::getOrganizationId)
                .findFirst()
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
