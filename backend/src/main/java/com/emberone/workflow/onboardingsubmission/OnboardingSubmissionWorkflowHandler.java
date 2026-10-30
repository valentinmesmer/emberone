/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.onboardingsubmission;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.domain.OnboardingInvitationWorkflowSubjects;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.service.OnboardingInvitationReadinessService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.onboarding.service.OnboardingPromotionService;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.DecisionType;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDecision;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OnboardingSubmissionWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class OnboardingSubmissionWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<OnboardingSubmissionWorkflowPayload> {

    public static final String SUBJECT_ENTITY_TYPE =
            OnboardingInvitationWorkflowSubjects.SUBMISSION_ENTITY_TYPE;

    private final OnboardingInvitationRepository onboardingInvitationRepository;
    private final OnboardingInvitationService onboardingInvitationService;
    private final OnboardingInvitationReadinessService onboardingInvitationReadinessService;
    private final OnboardingPromotionService onboardingPromotionService;
    private final UserService userService;
    private final WorkflowTypeMappingService workflowTypeMappingService;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public OnboardingSubmissionWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            OnboardingInvitationRepository onboardingInvitationRepository,
            OnboardingInvitationService onboardingInvitationService,
            OnboardingInvitationReadinessService onboardingInvitationReadinessService,
            OnboardingPromotionService onboardingPromotionService,
            UserService userService,
            WorkflowTypeMappingService workflowTypeMappingService,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.onboardingInvitationRepository = onboardingInvitationRepository;
        this.onboardingInvitationService = onboardingInvitationService;
        this.onboardingInvitationReadinessService = onboardingInvitationReadinessService;
        this.onboardingPromotionService = onboardingPromotionService;
        this.userService = userService;
        this.workflowTypeMappingService = workflowTypeMappingService;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ONBOARDING_SUBMISSION;
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
    public boolean allowsInputChangeRequests() {
        return false;
    }

    @Override
    public boolean mapsRejectToInputChange() {
        return true;
    }

    @Override
    public boolean allowsOpenerFreeInputEdit() {
        return false;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<OnboardingSubmissionWorkflowPayload> ctx, Workflow existing) {
        OnboardingSubmissionWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getInvitationId() == null) {
            return List.of();
        }
        OnboardingInvitation invitation =
                requireInvitation(ctx.getTenantId(), payload.getInvitationId());
        return List.of(
                WorkflowOpenConcurrencyKeys.organization(invitation.getTargetOrganizationId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_ONBOARDING_SUBMISSION_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open onboarding submission for this organization";
    }

    @Override
    public Class<OnboardingSubmissionWorkflowPayload> payloadType() {
        return OnboardingSubmissionWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "Onboarding submission is not possible";
    }

    /**
     * Context org is always the invitation's target org when known. Without a payload (pre-open
     * security checks) an open invitation takes precedence over any existing membership elsewhere.
     */
    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<OnboardingSubmissionWorkflowPayload> ctx) {
        return resolveInvitationTargetOrganization(
                        ctx.getOpenedByUserId(), ctx.getTenantId(), ctx.getPayload())
                .or(() -> findMembershipOrganization(ctx.getOpenedByUserId()))
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return findOpenInvitationTargetOrganization(openedByUserId)
                .or(() -> findMembershipOrganization(openedByUserId))
                .or(() -> findFirstEnabledOnboardingSubmissionOrganization())
                .orElseThrow(() -> new WorkflowValidationException(missingMembershipMessage()));
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        OnboardingSubmissionWorkflowPayload payload =
                readPayloadIfPresent(workflow, OnboardingSubmissionWorkflowPayload.class);
        if (payload != null && payload.getInvitationId() != null) {
            return requireInvitation(workflow.getTenantId(), payload.getInvitationId())
                    .getTargetOrganizationId();
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public void validateOpen(WorkflowOpenContext<OnboardingSubmissionWorkflowPayload> ctx) {
        validatePayload(ctx);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<OnboardingSubmissionWorkflowPayload> ctx, Workflow existing) {
        validatePayload(ctx);
    }

    @Override
    public void onOpened(Workflow workflow) {
        markInvitationSubmitted(workflow);
    }

    @Override
    public void onInputChangeRequested(Workflow workflow) {
        reopenInvitation(workflow);
    }

    @Override
    public void onInputChangeWithdrawn(Workflow workflow) {
        markInvitationSubmitted(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        markInvitationSubmitted(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        OnboardingSubmissionWorkflowPayload payload =
                readPayload(workflow, OnboardingSubmissionWorkflowPayload.class);
        OnboardingInvitation invitation =
                requireInvitation(workflow.getTenantId(), payload.getInvitationId());
        userService.ensureOrganizationMembershipAndAllMembersGroup(
                workflow.getTenantId(),
                workflow.getOpenedByUserId(),
                invitation.getTargetOrganizationId());
        Long approvedByUserId =
                workflow.getDecisions().stream()
                        .filter(decision -> decision.getDecisionType() == DecisionType.APPROVED)
                        .max(Comparator.comparing(WorkflowDecision::getDecidedAt))
                        .map(WorkflowDecision::getUserId)
                        .orElse(null);
        onboardingPromotionService.promoteAllSteps(invitation, approvedByUserId);
        onboardingInvitationService.markApproved(workflow.getTenantId(), payload.getInvitationId());
        log.info(
                "Approved onboarding invitation {} for user {} via workflow {}",
                invitation.getId(),
                workflow.getOpenedByUserId(),
                workflow.getId());
    }

    @Override
    public void onCancelled(Workflow workflow) {
        reopenInvitation(workflow);
    }

    private void reopenInvitation(Workflow workflow) {
        OnboardingSubmissionWorkflowPayload payload =
                readPayload(workflow, OnboardingSubmissionWorkflowPayload.class);
        onboardingInvitationService.reopen(
                workflow.getTenantId(), workflow.getOpenedByUserId(), payload.getInvitationId());
        log.info(
                "Reopened onboarding invitation {} after workflow {}",
                payload.getInvitationId(),
                workflow.getId());
    }

    private void markInvitationSubmitted(Workflow workflow) {
        OnboardingSubmissionWorkflowPayload payload =
                readPayload(workflow, OnboardingSubmissionWorkflowPayload.class);
        onboardingInvitationService.markSubmitted(
                workflow.getTenantId(), workflow.getOpenedByUserId(), payload.getInvitationId());
        log.info(
                "Marked onboarding invitation {} submitted via workflow {}",
                payload.getInvitationId(),
                workflow.getId());
    }

    private void validatePayload(WorkflowOpenContext<OnboardingSubmissionWorkflowPayload> ctx) {
        OnboardingSubmissionWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        if (payload.getInvitationId() == null) {
            throw new WorkflowValidationException("Onboarding invitation is required");
        }
        OnboardingInvitation invitation =
                requireOwnedInvitation(
                        ctx.getTenantId(), ctx.getOpenedByUserId(), payload.getInvitationId());
        if (invitation.getStatus() != OnboardingInvitationStatus.OPEN) {
            throw new WorkflowValidationException("Onboarding invitation is not open");
        }
        if (membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                ctx.getOpenedByUserId(), invitation.getTargetOrganizationId(), Instant.now())) {
            throw new WorkflowValidationException(
                    "You are already an active member of the target organization");
        }
        if (!onboardingInvitationReadinessService.isReadyToSubmit(
                ctx.getOpenedByUserId(), invitation.getId())) {
            throw new WorkflowValidationException("Onboarding is not complete");
        }
        workflowTypeMappingService.requireMapping(
                invitation.getTargetOrganizationId(), WorkflowType.ONBOARDING_SUBMISSION.getId());
        payload.setInvitationId(invitation.getId());
    }

    private Optional<Long> resolveInvitationTargetOrganization(
            Long userId, Long tenantId, OnboardingSubmissionWorkflowPayload payload) {
        if (payload != null && payload.getInvitationId() != null) {
            return Optional.of(
                    requireOwnedInvitation(tenantId, userId, payload.getInvitationId())
                            .getTargetOrganizationId());
        }
        return findOpenInvitationTargetOrganization(userId);
    }

    private OnboardingInvitation requireOwnedInvitation(
            Long tenantId, Long userId, Long invitationId) {
        OnboardingInvitation invitation =
                onboardingInvitationRepository
                        .findByIdAndUserId(invitationId, userId)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Onboarding invitation not found"));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new WorkflowValidationException(
                    "Onboarding invitation does not belong to tenant");
        }
        return invitation;
    }

    private OnboardingInvitation requireInvitation(Long tenantId, Long invitationId) {
        OnboardingInvitation invitation =
                onboardingInvitationRepository
                        .findById(invitationId)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Onboarding invitation not found"));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new WorkflowValidationException(
                    "Onboarding invitation does not belong to tenant");
        }
        return invitation;
    }

    private Optional<Long> findOpenInvitationTargetOrganization(Long userId) {
        return onboardingInvitationRepository
                .findFirstByUserIdAndStatusOrderByIdAsc(userId, OnboardingInvitationStatus.OPEN)
                .map(OnboardingInvitation::getTargetOrganizationId);
    }

    private Optional<Long> findFirstEnabledOnboardingSubmissionOrganization() {
        return workflowDefinitionRepository
                .findByWorkflowTypeId(WorkflowType.ONBOARDING_SUBMISSION.getId())
                .stream()
                .filter(WorkflowDefinition::isEnabled)
                .map(WorkflowDefinition::getOrganizationId)
                .findFirst();
    }
}
