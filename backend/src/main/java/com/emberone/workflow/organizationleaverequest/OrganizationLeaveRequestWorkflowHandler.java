/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationleaverequest;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserAccountContinuity;
import com.emberone.user.service.UserDeactivationService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.OrganizationMembershipExitService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
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
public class OrganizationLeaveRequestWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<OrganizationLeaveRequestWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.organizationLeaveRequest";

    private final WorkflowDocumentService workflowDocumentService;
    private final OrganizationRepository organizationRepository;
    private final WorkflowTypeMappingService workflowTypeMappingService;
    private final AllMembersGroupService allMembersGroupService;
    private final UserAccountContinuity userAccountContinuity;
    private final UserDeactivationService userDeactivationService;
    private final OrganizationMembershipExitService organizationMembershipExitService;

    public OrganizationLeaveRequestWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            OrganizationRepository organizationRepository,
            WorkflowTypeMappingService workflowTypeMappingService,
            AllMembersGroupService allMembersGroupService,
            UserAccountContinuity userAccountContinuity,
            UserDeactivationService userDeactivationService,
            OrganizationMembershipExitService organizationMembershipExitService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.organizationRepository = organizationRepository;
        this.workflowTypeMappingService = workflowTypeMappingService;
        this.allMembersGroupService = allMembersGroupService;
        this.userAccountContinuity = userAccountContinuity;
        this.userDeactivationService = userDeactivationService;
        this.organizationMembershipExitService = organizationMembershipExitService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ORGANIZATION_LEAVE_REQUEST;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx, Workflow existing) {
        OrganizationLeaveRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getOrganizationId() == null) {
            return List.of();
        }
        return List.of(WorkflowOpenConcurrencyKeys.organization(payload.getOrganizationId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_ORGANIZATION_LEAVE_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open leave request for this organization";
    }

    @Override
    public List<String> resolveOpenExclusivityKeys(
            WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx, Workflow existing) {
        OrganizationLeaveRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null
                || payload.getOrganizationId() == null
                || ctx.getOpenedByUserId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenExclusivityKeys.organizationMembershipExit(
                        ctx.getOpenedByUserId(), payload.getOrganizationId()));
    }

    @Override
    public String openExclusivityConflictCode() {
        return WorkflowValidationException.CODE_ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN;
    }

    @Override
    public String openExclusivityConflictMessage() {
        return "An organization membership exit is already in progress for this organization";
    }

    @Override
    public boolean requiresOpenerOrganizationMembership() {
        return false;
    }

    @Override
    public Class<OrganizationLeaveRequestWorkflowPayload> payloadType() {
        return OrganizationLeaveRequestWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - organization leave request is not possible";
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx) {
        OrganizationLeaveRequestWorkflowPayload payload = ctx.getPayload();
        if (payload != null && payload.getOrganizationId() != null) {
            return payload.getOrganizationId();
        }
        return findMembershipOrganization(ctx.getOpenedByUserId())
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return findMembershipOrganization(openedByUserId)
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        OrganizationLeaveRequestWorkflowPayload payload =
                readPayloadIfPresent(workflow, OrganizationLeaveRequestWorkflowPayload.class);
        if (payload != null && payload.getOrganizationId() != null) {
            return payload.getOrganizationId();
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        OrganizationLeaveRequestWorkflowPayload payload =
                readPayloadIfPresent(workflow, OrganizationLeaveRequestWorkflowPayload.class);
        String name = payload != null ? payload.getOrganizationName() : null;
        return resolveLocalizedDocumentPromotionMetadata(workflow, DOCUMENT_PROMOTION_PREFIX, name);
    }

    @Override
    public void validateOpen(WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx) {
        validatePayload(ctx, null);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx, Workflow existing) {
        validatePayload(ctx, existing);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachOrganizationLeaveRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachOrganizationLeaveRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        OrganizationLeaveRequestWorkflowPayload payload =
                readPayload(workflow, OrganizationLeaveRequestWorkflowPayload.class);
        Instant now = Instant.now();
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndOrganization_Id(
                                workflow.getOpenedByUserId(), payload.getOrganizationId(), now)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "You are not an active member of this organization"));
        rejectIfLeadershipMember(workflow.getOpenedByUserId(), payload.getOrganizationId());
        organizationMembershipExitService.applyApprovedExit(membership);
        log.info(
                "Ended organization membership {} for user {} in organization {} via workflow {}",
                membership.getId(),
                workflow.getOpenedByUserId(),
                payload.getOrganizationId(),
                workflow.getId());
    }

    private void validatePayload(
            WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx, Workflow existing) {
        OrganizationLeaveRequestWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);

        Long organizationId = payload.getOrganizationId();
        if (organizationId == null) {
            throw new WorkflowValidationException("Organization is required");
        }

        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!organization.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException("Organization does not belong to your tenant");
        }

        Instant now = Instant.now();
        if (!membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                ctx.getOpenedByUserId(), organizationId, now)) {
            throw new WorkflowValidationException(
                    "You are not an active member of this organization");
        }
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndOrganization_Id(
                                ctx.getOpenedByUserId(), organizationId, now)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "You are not an active member of this organization"));
        rejectIfLeadershipMember(ctx.getOpenedByUserId(), organizationId);
        if (userDeactivationService.hasDeactivationPending(ctx.getOpenedByUserId())) {
            throw new WorkflowValidationException(
                    "Account exit is already in progress",
                    WorkflowValidationException.CODE_USER_ALREADY_DEACTIVATED);
        }
        Instant exitAt = Instant.now();
        if (!Boolean.TRUE.equals(membership.getMain())) {
            userAccountContinuity.requireSustainableAfterEndingOrganizationMembershipForWorkflow(
                    ctx.getOpenedByUserId(), organizationId, exitAt);
        }

        workflowTypeMappingService.requireMapping(
                organizationId, WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId());

        String reason = trimToNull(payload.getReason());
        if (reason == null) {
            throw new WorkflowValidationException("Reason is required");
        }
        if (reason.length() > OrganizationLeaveRequestWorkflowPayload.MAX_REASON_LENGTH) {
            throw new WorkflowValidationException(
                    "Reason must be at most "
                            + OrganizationLeaveRequestWorkflowPayload.MAX_REASON_LENGTH
                            + " characters");
        }

        payload.setOrganizationId(organizationId);
        payload.setOrganizationName(organization.getName());
        payload.setReason(reason);

        Long membershipOrgId = findMembershipOrganization(ctx.getOpenedByUserId()).orElse(null);
        if (existing == null) {
            validateWorkflowDraftAttachments(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    payload.getAttachmentIds(),
                    OrganizationLeaveRequestWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        } else {
            validateWorkflowAttachmentsForUpdate(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    existing.getId(),
                    payload.getAttachmentIds(),
                    OrganizationLeaveRequestWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        }
    }

    private void rejectIfLeadershipMember(Long userId, Long organizationId) {
        if (allMembersGroupService.hasActiveLeadershipMembership(userId, organizationId)) {
            throw new WorkflowValidationException(
                    "You cannot leave this organization while you are in the leadership group",
                    WorkflowValidationException.CODE_LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE);
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
