/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationgroupjoinrequest;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.exception.OrganizationGroupNotFoundException;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationGroupJoinRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
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
public class OrganizationGroupJoinRequestWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<
                OrganizationGroupJoinRequestWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.organizationGroupJoinRequest";

    private final WorkflowDocumentService workflowDocumentService;
    private final UserOrganizationGroupMembershipService groupMembershipService;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final WorkflowTypeMappingService workflowTypeMappingService;

    public OrganizationGroupJoinRequestWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            UserOrganizationGroupMembershipService groupMembershipService,
            UserOrganizationGroupMembershipRepository groupMembershipRepository,
            OrganizationGroupRepository organizationGroupRepository,
            WorkflowTypeMappingService workflowTypeMappingService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.groupMembershipService = groupMembershipService;
        this.groupMembershipRepository = groupMembershipRepository;
        this.organizationGroupRepository = organizationGroupRepository;
        this.workflowTypeMappingService = workflowTypeMappingService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST;
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
            WorkflowOpenContext<OrganizationGroupJoinRequestWorkflowPayload> ctx,
            Workflow existing) {
        OrganizationGroupJoinRequestWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getOrganizationGroupId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenConcurrencyKeys.organizationGroup(payload.getOrganizationGroupId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_ORGANIZATION_GROUP_JOIN_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open join request for this organization group";
    }

    @Override
    public Class<OrganizationGroupJoinRequestWorkflowPayload> payloadType() {
        return OrganizationGroupJoinRequestWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - organization group join request is not possible";
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<OrganizationGroupJoinRequestWorkflowPayload> ctx) {
        OrganizationGroupJoinRequestWorkflowPayload payload = ctx.getPayload();
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
        OrganizationGroupJoinRequestWorkflowPayload payload =
                readPayloadIfPresent(workflow, OrganizationGroupJoinRequestWorkflowPayload.class);
        if (payload != null && payload.getOrganizationId() != null) {
            return payload.getOrganizationId();
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        OrganizationGroupJoinRequestWorkflowPayload payload =
                readPayloadIfPresent(workflow, OrganizationGroupJoinRequestWorkflowPayload.class);
        String name = payload != null ? payload.getOrganizationGroupName() : null;
        return resolveLocalizedDocumentPromotionMetadata(workflow, DOCUMENT_PROMOTION_PREFIX, name);
    }

    @Override
    public void validateOpen(WorkflowOpenContext<OrganizationGroupJoinRequestWorkflowPayload> ctx) {
        validatePayload(ctx, null);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<OrganizationGroupJoinRequestWorkflowPayload> ctx,
            Workflow existing) {
        validatePayload(ctx, existing);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachOrganizationGroupJoinRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachOrganizationGroupJoinRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        OrganizationGroupJoinRequestWorkflowPayload payload =
                readPayload(workflow, OrganizationGroupJoinRequestWorkflowPayload.class);
        Instant now = Instant.now();
        groupMembershipService.createMembership(
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(workflow.getOpenedByUserId())
                        .organizationGroupId(payload.getOrganizationGroupId())
                        .startDate(now)
                        .membershipType(GroupMembershipType.MEMBER)
                        .build());
        log.info(
                "Created organization group membership for user {} in group {} via workflow {}",
                workflow.getOpenedByUserId(),
                payload.getOrganizationGroupId(),
                workflow.getId());
    }

    private void validatePayload(
            WorkflowOpenContext<OrganizationGroupJoinRequestWorkflowPayload> ctx,
            Workflow existing) {
        OrganizationGroupJoinRequestWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);

        Long organizationGroupId = payload.getOrganizationGroupId();
        if (organizationGroupId == null) {
            throw new WorkflowValidationException("Organization group is required");
        }

        if (existing != null) {
            // Approver slots are resolved once at open; a different group needs its own workflow.
            OrganizationGroupJoinRequestWorkflowPayload previous =
                    readPayloadIfPresent(
                            existing, OrganizationGroupJoinRequestWorkflowPayload.class);
            if (previous != null
                    && previous.getOrganizationGroupId() != null
                    && !previous.getOrganizationGroupId().equals(organizationGroupId)) {
                throw new WorkflowValidationException(
                        "Organization group cannot be changed; cancel and open a new request");
            }
        }

        OrganizationGroup group =
                organizationGroupRepository
                        .findById(organizationGroupId)
                        .orElseThrow(
                                () -> new OrganizationGroupNotFoundException(organizationGroupId));
        if (!group.getTenantId().equals(ctx.getTenantId())) {
            throw new WorkflowValidationException(
                    "Organization group does not belong to your tenant");
        }
        if (group.getType() == OrganizationGroupType.ALL_MEMBERS) {
            throw new WorkflowValidationException(
                    "You cannot join the all-members organization group");
        }
        if (group.getType() == OrganizationGroupType.LEADERSHIP) {
            throw new WorkflowValidationException(
                    "You cannot join the leadership organization group",
                    WorkflowValidationException.CODE_LEADERSHIP_GROUP_JOIN_NOT_ALLOWED);
        }
        if (group.isArchived()) {
            throw new WorkflowValidationException("Organization group is archived");
        }

        Long organizationId = group.getOrganization().getId();
        if (payload.getOrganizationId() != null
                && !organizationId.equals(payload.getOrganizationId())) {
            throw new WorkflowValidationException(
                    "Organization group does not belong to the selected organization");
        }

        Instant now = Instant.now();
        if (!membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                ctx.getOpenedByUserId(), organizationId, now)) {
            throw new WorkflowValidationException(
                    "You must be an active member of the organization to join this group");
        }
        if (groupMembershipRepository
                .findActiveByUser_IdAndOrganizationGroup_Id(
                        ctx.getOpenedByUserId(), organizationGroupId, now)
                .isPresent()) {
            throw new WorkflowValidationException(
                    "You are already an active member of this organization group");
        }

        workflowTypeMappingService.requireMapping(
                organizationId, WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST.getId());

        String reason = trimToNull(payload.getReason());
        if (reason != null
                && reason.length()
                        > OrganizationGroupJoinRequestWorkflowPayload.MAX_REASON_LENGTH) {
            throw new WorkflowValidationException(
                    "Reason must be at most "
                            + OrganizationGroupJoinRequestWorkflowPayload.MAX_REASON_LENGTH
                            + " characters");
        }

        payload.setOrganizationId(organizationId);
        payload.setOrganizationName(group.getOrganization().getName());
        payload.setOrganizationGroupId(organizationGroupId);
        payload.setOrganizationGroupName(group.getName());
        payload.setReason(reason);

        Long membershipOrgId = findMembershipOrganization(ctx.getOpenedByUserId()).orElse(null);
        if (existing == null) {
            validateWorkflowDraftAttachments(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    payload.getAttachmentIds(),
                    OrganizationGroupJoinRequestWorkflowPayload.MAX_ATTACHMENTS,
                    false);
        } else {
            validateWorkflowAttachmentsForUpdate(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    existing.getId(),
                    payload.getAttachmentIds(),
                    OrganizationGroupJoinRequestWorkflowPayload.MAX_ATTACHMENTS,
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
