/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationusermainorganizationchange;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationUserMainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.ApproverOrg;
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
public class OrganizationUserMainOrganizationChangeWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<
                OrganizationUserMainOrganizationChangeWorkflowPayload> {

    public static final String SUBJECT_ENTITY_TYPE = "USER";

    private final OrganizationRepository organizationRepository;
    private final OrganizationAccessService organizationAccessService;
    private final WorkflowTypeMappingService workflowTypeMappingService;
    private final WorkflowRepository workflowRepository;
    private final UserOrganizationMembershipService membershipService;

    public OrganizationUserMainOrganizationChangeWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            OrganizationRepository organizationRepository,
            OrganizationAccessService organizationAccessService,
            WorkflowTypeMappingService workflowTypeMappingService,
            WorkflowRepository workflowRepository,
            UserOrganizationMembershipService membershipService,
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
        this.membershipService = membershipService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE;
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
    public Class<OrganizationUserMainOrganizationChangeWorkflowPayload> payloadType() {
        return OrganizationUserMainOrganizationChangeWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "Main organization change is not possible for this member";
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx,
            Workflow existing) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getTargetUserId() == null) {
            return List.of();
        }
        return List.of(WorkflowOpenConcurrencyKeys.user(payload.getTargetUserId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "A main organization change request is already open for this member";
    }

    @Override
    public List<String> resolveOpenExclusivityKeys(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx,
            Workflow existing) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getTargetUserId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenExclusivityKeys.userMainOrganizationChange(payload.getTargetUserId()));
    }

    @Override
    public String openExclusivityConflictMessage() {
        return "A main organization change is already in progress for this member";
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload = requirePayload(ctx);
        Long targetUserId = payload.getTargetUserId();
        return findMainOrganization(targetUserId)
                .orElseGet(
                        () -> {
                            if (payload.getNewMainOrganizationId() != null) {
                                return payload.getNewMainOrganizationId();
                            }
                            throw new WorkflowValidationException(missingMembershipMessage());
                        });
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        throw new WorkflowValidationException(missingMembershipMessage());
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload =
                readPayloadIfPresent(
                        workflow, OrganizationUserMainOrganizationChangeWorkflowPayload.class);
        if (payload == null || payload.getTargetUserId() == null) {
            throw new WorkflowValidationException(missingMembershipMessage());
        }
        return findMainOrganization(payload.getTargetUserId())
                .orElseGet(
                        () -> {
                            if (payload.getNewMainOrganizationId() != null) {
                                return payload.getNewMainOrganizationId();
                            }
                            throw new WorkflowValidationException(missingMembershipMessage());
                        });
    }

    @Override
    public List<ApproverOrg> resolveDynamicOrganizations(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload = requirePayload(ctx);
        if (payload.getNewMainOrganizationId() == null) {
            return List.of();
        }
        return List.of(new ApproverOrg(payload.getNewMainOrganizationId()));
    }

    @Override
    public void validateOpen(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx) {
        validatePayload(ctx);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx,
            Workflow existing) {
        validatePayload(ctx);
    }

    @Override
    public void onApproved(Workflow workflow) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload =
                readPayload(workflow, OrganizationUserMainOrganizationChangeWorkflowPayload.class);
        membershipService.changeMainOrganization(
                payload.getTargetUserId(), payload.getNewMainOrganizationId());
        log.info(
                "Changed main organization for user {} to organization {} via org workflow {}",
                payload.getTargetUserId(),
                payload.getNewMainOrganizationId(),
                workflow.getId());
    }

    private void validatePayload(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);

        Long organizationId = payload.getOrganizationId();
        Long targetUserId = payload.getTargetUserId();
        if (organizationId == null || targetUserId == null) {
            throw new WorkflowValidationException("Organization and target user are required");
        }

        if (!organizationAccessService.canRequestMainOrganizationChangeForUser(
                organizationId, targetUserId)) {
            throw new WorkflowValidationException("Main organization change is not allowed");
        }

        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!organization.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException("Organization does not belong to your tenant");
        }

        User targetUser = requireTargetUser(ctx.getTenantId(), targetUserId);
        Instant now = Instant.now();
        UserOrganizationMembership contextMembership =
                membershipRepository
                        .findActiveByUser_IdAndOrganization_Id(targetUserId, organizationId, now)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Target user is not an active member of this"
                                                        + " organization"));

        Long newMainOrganizationId = payload.getNewMainOrganizationId();
        if (newMainOrganizationId == null) {
            throw new WorkflowValidationException("New main organization is required");
        }
        final Long resolvedNewMainOrganizationId = newMainOrganizationId;

        Long currentMainOrgId = findMainOrganization(targetUserId).orElse(null);
        if (currentMainOrgId != null && resolvedNewMainOrganizationId.equals(currentMainOrgId)) {
            throw new WorkflowValidationException(
                    "New main organization must differ from the member's current main"
                            + " organization");
        }

        Organization newMainOrganization =
                organizationRepository
                        .findById(resolvedNewMainOrganizationId)
                        .orElseThrow(
                                () ->
                                        new OrganizationNotFoundException(
                                                resolvedNewMainOrganizationId));
        if (!newMainOrganization.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException(
                    "New main organization does not belong to your tenant");
        }

        if (!membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                targetUserId, resolvedNewMainOrganizationId, now)) {
            throw new WorkflowValidationException(
                    "Target user must be an active member of the new main organization");
        }

        if (contextMembership.getEndDate() != null
                || membershipRepository
                                .findActiveByUser_IdAndOrganization_Id(
                                        targetUserId, resolvedNewMainOrganizationId, now)
                                .map(UserOrganizationMembership::getEndDate)
                                .orElse(null)
                        != null) {
            throw new WorkflowValidationException(
                    "Cannot change main organization while a membership end date is set");
        }

        workflowTypeMappingService.requireMapping(
                organizationId, WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE.getId());

        if (workflowRepository.existsActiveByWorkflowTypeIdAndSubject(
                WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE.getId(),
                SUBJECT_ENTITY_TYPE,
                targetUserId)) {
            throw new WorkflowValidationException(
                    openConcurrencyConflictMessage(), openConcurrencyConflictCode());
        }

        payload.setOrganizationId(organizationId);
        payload.setTargetUserId(targetUserId);
        payload.setNewMainOrganizationId(resolvedNewMainOrganizationId);
        payload.setNewMainOrganizationName(newMainOrganization.getName());
        payload.setOrganizationName(organization.getName());
        payload.setTargetUserDisplayName(formatUserDisplayName(targetUser));
        payload.setReason(trimToNull(payload.getReason()));
    }

    private OrganizationUserMainOrganizationChangeWorkflowPayload requirePayload(
            WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> ctx) {
        OrganizationUserMainOrganizationChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        return payload;
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
            throw new WorkflowValidationException("Target user is not active");
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
