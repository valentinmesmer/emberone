/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.drivinglicense;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class DrivingLicenseRecordWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<DrivingLicenseRecordWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.drivingLicenseRecord";

    private final WorkflowDocumentService workflowDocumentService;
    private final DrivingLicenseRecordService drivingLicenseRecordService;
    private final DrivingLicenseCatalogService catalogService;
    private final TenantDrivingLicenseDefinitionRepository tenantDefinitionRepository;

    public DrivingLicenseRecordWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            DrivingLicenseRecordService drivingLicenseRecordService,
            DrivingLicenseCatalogService catalogService,
            TenantDrivingLicenseDefinitionRepository tenantDefinitionRepository,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.drivingLicenseRecordService = drivingLicenseRecordService;
        this.catalogService = catalogService;
        this.tenantDefinitionRepository = tenantDefinitionRepository;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.DRIVING_LICENSE_RECORD;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return false;
    }

    @Override
    public Class<DrivingLicenseRecordWorkflowPayload> payloadType() {
        return DrivingLicenseRecordWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - driving license submission is not possible";
    }

    @Override
    public Optional<com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata>
            resolveDocumentPromotionMetadata(Workflow workflow) {
        return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                workflow, DOCUMENT_PROMOTION_PREFIX);
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return requireMembershipOrganization(openedByUserId);
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<DrivingLicenseRecordWorkflowPayload> ctx) {
        DrivingLicenseRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        validateClassDefinitions(payload, requireMembershipOrganization(ctx.getOpenedByUserId()));
        return requireMembershipOrganization(ctx.getOpenedByUserId());
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        DrivingLicenseRecordWorkflowPayload payload =
                readPayloadIfPresent(workflow, DrivingLicenseRecordWorkflowPayload.class);
        if (payload != null) {
            return resolveContextOrganization(
                    new WorkflowOpenContext<>(
                            workflow.getOpenedByUserId(), workflow.getTenantId(), payload));
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public void validateOpen(WorkflowOpenContext<DrivingLicenseRecordWorkflowPayload> ctx) {
        DrivingLicenseRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        Long membershipOrgId = requireMembershipOrganization(ctx.getOpenedByUserId());
        validatePayload(payload);
        validateClassDefinitions(payload, membershipOrgId);
        validateWorkflowDraftAttachments(
                ctx.getOpenedByUserId(),
                membershipOrgId,
                payload.getAttachmentIds(),
                DrivingLicenseRecordWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<DrivingLicenseRecordWorkflowPayload> ctx, Workflow existing) {
        DrivingLicenseRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        Long membershipOrgId = requireMembershipOrganization(ctx.getOpenedByUserId());
        validatePayload(payload);
        validateClassDefinitions(payload, membershipOrgId);
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                membershipOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                DrivingLicenseRecordWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachDrivingLicenseAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachDrivingLicenseAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        DrivingLicenseRecordWorkflowPayload payload =
                readPayload(workflow, DrivingLicenseRecordWorkflowPayload.class);
        drivingLicenseRecordService.upsertFromWorkflow(
                workflow.getTenantId(), workflow.getOpenedByUserId(), payload, workflow.getId());
        log.info(
                "Upserted driving license for user {} via approved workflow {}",
                workflow.getOpenedByUserId(),
                workflow.getId());
    }

    @Override
    public void onDocumentsPromoted(Workflow workflow, List<Long> documentIds) {
        drivingLicenseRecordService.linkDocumentsFromWorkflow(
                workflow.getTenantId(), workflow.getId(), documentIds);
    }

    private void validatePayload(DrivingLicenseRecordWorkflowPayload payload) {
        if (payload.getIssueDate() == null) {
            throw new WorkflowValidationException("issueDate is required");
        }
        if (!StringUtils.hasText(payload.getLicenseNumber())) {
            throw new WorkflowValidationException("licenseNumber is required");
        }
        if (!StringUtils.hasText(payload.getIssuingAuthority())) {
            throw new WorkflowValidationException("issuingAuthority is required");
        }
        if (payload.getClasses() == null || payload.getClasses().isEmpty()) {
            throw new WorkflowValidationException("At least one driving license class is required");
        }
        Set<Long> uniqueDefinitionIds = new HashSet<>();
        for (DrivingLicenseRecordWorkflowPayload.DrivingLicenseClassPayload classPayload :
                payload.getClasses()) {
            if (classPayload.getTenantDrivingLicenseDefinitionId() == null) {
                throw new WorkflowValidationException(
                        "tenantDrivingLicenseDefinitionId is required");
            }
            if (!uniqueDefinitionIds.add(classPayload.getTenantDrivingLicenseDefinitionId())) {
                throw new WorkflowValidationException("Duplicate driving license class in request");
            }
        }
    }

    private void validateClassDefinitions(
            DrivingLicenseRecordWorkflowPayload payload, Long membershipOrgId) {
        for (DrivingLicenseRecordWorkflowPayload.DrivingLicenseClassPayload classPayload :
                payload.getClasses()) {
            TenantDrivingLicenseDefinition definition =
                    requireDefinition(classPayload.getTenantDrivingLicenseDefinitionId());
            if (!LifecycleFields.isCatalogSelectable(definition)) {
                throw new WorkflowValidationException(
                        definition.isArchived()
                                ? "Driving license definition is archived"
                                : "Driving license definition is not active");
            }
            try {
                catalogService.requireDefinitionAvailableForOrg(
                        definition.getId(), membershipOrgId);
            } catch (RuntimeException ex) {
                throw new WorkflowValidationException(ex.getMessage());
            }
        }
    }

    private TenantDrivingLicenseDefinition requireDefinition(
            Long tenantDrivingLicenseDefinitionId) {
        if (tenantDrivingLicenseDefinitionId == null) {
            throw new WorkflowValidationException("tenantDrivingLicenseDefinitionId is required");
        }
        return tenantDefinitionRepository
                .findById(tenantDrivingLicenseDefinitionId)
                .orElseThrow(
                        () ->
                                new WorkflowValidationException(
                                        "Driving license definition not found: "
                                                + tenantDrivingLicenseDefinitionId));
    }
}
