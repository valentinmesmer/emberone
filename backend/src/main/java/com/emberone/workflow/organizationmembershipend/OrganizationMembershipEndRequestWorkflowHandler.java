/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationmembershipend;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserAccountContinuity;
import com.emberone.user.service.UserDeactivationService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.OrganizationMembershipExitService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationMembershipEndRequestWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import com.emberone.workflow.spi.WorkflowOpenExclusivityKeys;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class OrganizationMembershipEndRequestWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<
                OrganizationMembershipEndRequestWorkflowPayload> {

    public static final String SUBJECT_ENTITY_TYPE = "USER";

    private final OrganizationRepository organizationRepository;
    private final OrganizationAccessService organizationAccessService;
    private final WorkflowTypeMappingService workflowTypeMappingService;
    private final WorkflowRepository workflowRepository;
    private final AllMembersGroupService allMembersGroupService;
    private final UserAccountContinuity userAccountContinuity;
    private final UserDeactivationService userDeactivationService;
    private final OrganizationMembershipExitService organizationMembershipExitService;

    public OrganizationMembershipEndRequestWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            OrganizationRepository organizationRepository,
            OrganizationAccessService organizationAccessService,
            WorkflowTypeMappingService workflowTypeMappingService,
            WorkflowRepository workflowRepository,
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
        this.organizationRepository = organizationRepository;
        this.organizationAccessService = organizationAccessService;
        this.workflowTypeMappingService = workflowTypeMappingService;
        this.workflowRepository = workflowRepository;
        this.allMembersGroupService = allMembersGroupService;
        this.userAccountContinuity = userAccountContinuity;
        this.userDeactivationService = userDeactivationService;
        this.organizationMembershipExitService = organizationMembershipExitService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ORGANIZATION_MEMBERSHIP_END_REQUEST;
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
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> ctx,
            Workflow existing) {
        OrganizationMembershipEndRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null
                || payload.getOrganizationId() == null
                || payload.getTargetUserId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenConcurrencyKeys.organization(payload.getOrganizationId()),
                WorkflowOpenConcurrencyKeys.user(payload.getTargetUserId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "An organization membership end request is already open for this member";
    }

    @Override
    public List<String> resolveOpenExclusivityKeys(
            WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> ctx,
            Workflow existing) {
        OrganizationMembershipEndRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null
                || payload.getOrganizationId() == null
                || payload.getTargetUserId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenExclusivityKeys.organizationMembershipExit(
                        payload.getTargetUserId(), payload.getOrganizationId()));
    }

    @Override
    public String openExclusivityConflictCode() {
        return WorkflowValidationException.CODE_ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN;
    }

    @Override
    public String openExclusivityConflictMessage() {
        return "An organization membership exit is already in progress for this member";
    }

    @Override
    public Class<OrganizationMembershipEndRequestWorkflowPayload> payloadType() {
        return OrganizationMembershipEndRequestWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "Organization membership end request is not possible";
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> ctx) {
        OrganizationMembershipEndRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getOrganizationId() == null) {
            throw new WorkflowValidationException("Organization is required");
        }
        return payload.getOrganizationId();
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        throw new WorkflowValidationException(missingMembershipMessage());
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        OrganizationMembershipEndRequestWorkflowPayload payload =
                readPayloadIfPresent(
                        workflow, OrganizationMembershipEndRequestWorkflowPayload.class);
        if (payload != null && payload.getOrganizationId() != null) {
            return payload.getOrganizationId();
        }
        throw new WorkflowValidationException(missingMembershipMessage());
    }

    @Override
    public void validateOpen(
            WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> ctx) {
        validatePayload(ctx);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> ctx,
            Workflow existing) {
        validatePayload(ctx);
    }

    @Override
    public void onApproved(Workflow workflow) {
        OrganizationMembershipEndRequestWorkflowPayload payload =
                readPayload(workflow, OrganizationMembershipEndRequestWorkflowPayload.class);
        Instant now = Instant.now();
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndOrganization_Id(
                                payload.getTargetUserId(), payload.getOrganizationId(), now)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Target user is not an active member of this organization"));
        rejectIfLeadershipMember(payload.getTargetUserId(), payload.getOrganizationId());
        organizationMembershipExitService.applyApprovedExit(membership);
        log.info(
                "Ended organization membership {} for user {} via workflow {} (opened by {})",
                membership.getId(),
                payload.getTargetUserId(),
                workflow.getId(),
                workflow.getOpenedByUserId());
    }

    private void validatePayload(
            WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> ctx) {
        OrganizationMembershipEndRequestWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);

        Long organizationId = payload.getOrganizationId();
        Long targetUserId = payload.getTargetUserId();
        if (organizationId == null || targetUserId == null) {
            throw new WorkflowValidationException("Organization and target user are required");
        }

        if (!organizationAccessService.canRequestOrganizationMembershipEnd(
                organizationId, targetUserId)) {
            throw new WorkflowValidationException("Ending organization membership is not allowed");
        }

        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!organization.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException("Organization does not belong to your tenant");
        }

        User targetUser = requireTargetUser(ctx.getTenantId(), targetUserId);
        rejectIfLeadershipMember(targetUserId, organizationId);

        Instant now = Instant.now();
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndOrganization_Id(targetUserId, organizationId, now)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Target user is not an active member of this organization"));
        if (userDeactivationService.hasDeactivationPending(targetUserId)) {
            throw new WorkflowValidationException(
                    "Account exit is already in progress",
                    WorkflowValidationException.CODE_USER_ALREADY_DEACTIVATED);
        }
        Instant exitAt = Instant.now();
        if (!Boolean.TRUE.equals(membership.getMain())) {
            userAccountContinuity.requireSustainableAfterEndingOrganizationMembershipForWorkflow(
                    targetUserId, organizationId, exitAt);
        }

        if (workflowRepository.existsActiveByWorkflowTypeIdAndSubject(
                WorkflowType.ORGANIZATION_MEMBERSHIP_END_REQUEST.getId(),
                SUBJECT_ENTITY_TYPE,
                targetUserId)) {
            throw new WorkflowValidationException(
                    openConcurrencyConflictMessage(), openConcurrencyConflictCode());
        }

        workflowTypeMappingService.requireMapping(
                organizationId, WorkflowType.ORGANIZATION_MEMBERSHIP_END_REQUEST.getId());

        payload.setOrganizationId(organizationId);
        payload.setTargetUserId(targetUserId);
        payload.setOrganizationName(organization.getName());
        payload.setTargetUserDisplayName(formatUserDisplayName(targetUser));
        String reason = trimToNull(payload.getReason());
        if (reason == null) {
            throw new WorkflowValidationException("Reason is required");
        }
        if (reason.length() > OrganizationMembershipEndRequestWorkflowPayload.MAX_REASON_LENGTH) {
            throw new WorkflowValidationException(
                    "Reason must be at most "
                            + OrganizationMembershipEndRequestWorkflowPayload.MAX_REASON_LENGTH
                            + " characters");
        }
        payload.setReason(reason);
    }

    private void rejectIfLeadershipMember(Long userId, Long organizationId) {
        if (allMembersGroupService.hasActiveLeadershipMembership(userId, organizationId)) {
            throw new WorkflowValidationException(
                    "Cannot end organization membership while the user is in the leadership group",
                    WorkflowValidationException.CODE_LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE);
        }
    }

    private User requireTargetUser(Long tenantId, Long targetUserId) {
        User targetUser =
                userRepository
                        .findById(targetUserId)
                        .orElseThrow(
                                () -> new WorkflowValidationException("Target user not found"));
        if (!targetUser.getTenantId().equals(tenantId)) {
            throw new WorkflowValidationException("Target user does not belong to your tenant");
        }
        if (targetUser.getDeactivatedAt() != null) {
            throw new WorkflowValidationException("User is already deactivated");
        }
        return targetUser;
    }

    private static String formatUserDisplayName(User user) {
        String name =
                ((user.getFirstName() != null ? user.getFirstName() : "")
                                + " "
                                + (user.getLastName() != null ? user.getLastName() : ""))
                        .trim();
        if (!name.isEmpty()) {
            return name;
        }
        return user.getUsername() != null ? user.getUsername().trim() : "";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
