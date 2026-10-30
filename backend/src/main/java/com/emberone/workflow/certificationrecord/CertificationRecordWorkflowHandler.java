/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.certificationrecord;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.certification.domain.CertEvidenceType;
import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationCatalogService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.certification.service.TenantCertDefinitionEvidenceTypeService;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.CertificationRecordWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Handles {@link WorkflowType#CERTIFICATION_RECORD}: a user submits proof for a single
 * certification definition with a completion date and documents. On approval, a missing assignment
 * is created and one {@code UserCertificationRecord} is written.
 */
@Slf4j
@Component
public class CertificationRecordWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<CertificationRecordWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.certificationRecord";

    private final WorkflowDocumentService workflowDocumentService;
    private final CertificationRecordService certificationRecordService;
    private final CertificationAssignmentService assignmentService;
    private final CertificationCatalogService catalogService;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService;

    public CertificationRecordWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            CertificationRecordService certificationRecordService,
            CertificationAssignmentService assignmentService,
            CertificationCatalogService catalogService,
            TenantCertDefinitionRepository tenantDefinitionRepository,
            TenantCertDefinitionEvidenceTypeService evidenceTypeService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.certificationRecordService = certificationRecordService;
        this.assignmentService = assignmentService;
        this.catalogService = catalogService;
        this.tenantDefinitionRepository = tenantDefinitionRepository;
        this.evidenceTypeService = evidenceTypeService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.CERTIFICATION_RECORD;
    }

    /**
     * Multiple certification submissions may be open at once, but definition ids must not overlap.
     */
    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<CertificationRecordWorkflowPayload> ctx, Workflow existing) {
        CertificationRecordWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getTenantCertDefinitionId() == null) {
            return List.of();
        }
        return List.of(
                WorkflowOpenConcurrencyKeys.certificationDefinition(
                        payload.getTenantCertDefinitionId()));
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_CERTIFICATION_DEFINITION_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open certification submission for this definition; "
                + "finish or cancel it before opening another";
    }

    @Override
    public Class<CertificationRecordWorkflowPayload> payloadType() {
        return CertificationRecordWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - certification submission is not possible";
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        String definitionName = resolveCertificationDefinitionName(workflow);
        if (StringUtils.hasText(definitionName)) {
            return documentPromotionMetadataSupport.fromInterpolatedTitle(
                    workflow, DOCUMENT_PROMOTION_PREFIX, Map.of("name", definitionName.trim()));
        }
        return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                workflow, DOCUMENT_PROMOTION_PREFIX);
    }

    /**
     * Pre-open checks (draft upload, {@code canBeginSelfServiceWorkflow}) have no payload yet - use
     * membership org like other self-service workflows.
     */
    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return requireMembershipOrganization(openedByUserId);
    }

    /** Approval context follows the user's membership organization. */
    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<CertificationRecordWorkflowPayload> ctx) {
        CertificationRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        Long definitionId = requireDefinitionId(payload);
        requireDefinition(definitionId);
        return requireMembershipOrganization(ctx.getOpenedByUserId());
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        CertificationRecordWorkflowPayload payload =
                readPayloadIfPresent(workflow, CertificationRecordWorkflowPayload.class);
        if (payload != null) {
            return resolveContextOrganization(
                    new WorkflowOpenContext<>(
                            workflow.getOpenedByUserId(), workflow.getTenantId(), payload));
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public void validateOpen(WorkflowOpenContext<CertificationRecordWorkflowPayload> ctx) {
        validatePayload(ctx, null);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<CertificationRecordWorkflowPayload> ctx, Workflow existing) {
        validatePayload(ctx, existing);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachCertificationAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachCertificationAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        CertificationRecordWorkflowPayload payload =
                readPayload(workflow, CertificationRecordWorkflowPayload.class);
        Long definitionId = requireDefinitionId(payload);
        assignmentService.createAssignmentsFromWorkflow(
                workflow.getTenantId(),
                workflow.getOpenedByUserId(),
                workflow.getId(),
                List.of(definitionId));
        certificationRecordService.createFromWorkflow(
                workflow.getTenantId(),
                workflow.getOpenedByUserId(),
                List.of(definitionId),
                payload.getCompletedDate(),
                payload.getNotes(),
                workflow.getId());
        log.info(
                "Created certification record for user {} via approved workflow {}",
                workflow.getOpenedByUserId(),
                workflow.getId());
    }

    @Override
    public void onRejected(Workflow workflow) {
        workflowDocumentService.deleteCertificationProofAttachments(workflow);
    }

    @Override
    public void onCancelled(Workflow workflow) {
        workflowDocumentService.deleteCertificationProofAttachments(workflow);
    }

    @Override
    public void onDocumentsPromoted(Workflow workflow, List<Long> documentIds) {
        certificationRecordService.linkDocumentsFromWorkflow(
                workflow.getTenantId(), workflow.getId(), documentIds);
        workflowDocumentService.deleteCertificationProofAttachments(workflow);
    }

    private void validatePayload(
            WorkflowOpenContext<CertificationRecordWorkflowPayload> ctx, Workflow existing) {
        CertificationRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        Long definitionId = requireDefinitionId(payload);
        if (payload.getCompletedDate() == null) {
            throw new WorkflowValidationException("completedDate is required");
        }
        requireOpeningUser(ctx);
        Long membershipOrgId = requireMembershipOrganization(ctx.getOpenedByUserId());
        TenantCertDefinition definition = requireDefinition(definitionId);
        if (!LifecycleFields.isCatalogSelectable(definition)) {
            throw new WorkflowValidationException(
                    definition.isArchived()
                            ? "Certification definition is archived"
                            : "Certification definition is not active");
        }
        catalogService.requireTypeAvailableForOrg(definitionId, membershipOrgId);
        Set<CertEvidenceType> evidenceTypes = evidenceTypeService.getEvidenceTypes(definitionId);
        if (!TenantCertDefinitionEvidenceTypeService.allowsWorkflowSubmission(evidenceTypes)) {
            throw new WorkflowValidationException(
                    "Self-service submission is not available for this certification definition");
        }
        try {
            certificationRecordService.resolveExpiryForCapture(
                    definition, payload.getCompletedDate(), null);
        } catch (ConflictException ex) {
            throw new WorkflowValidationException(ex.getMessage());
        }
        boolean attachmentRequired =
                TenantCertDefinitionEvidenceTypeService.requiresWorkflowAttachment(evidenceTypes);
        if (existing == null) {
            validateWorkflowDraftAttachments(
                    ctx.getOpenedByUserId(),
                    membershipOrgId,
                    payload.getAttachmentIds(),
                    CertificationRecordWorkflowPayload.MAX_ATTACHMENTS,
                    attachmentRequired);
            return;
        }
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                membershipOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                CertificationRecordWorkflowPayload.MAX_ATTACHMENTS,
                attachmentRequired);
    }

    private static Long requireDefinitionId(CertificationRecordWorkflowPayload payload) {
        Long definitionId = payload.getTenantCertDefinitionId();
        if (definitionId == null) {
            throw new WorkflowValidationException("tenantCertDefinitionId is required");
        }
        return definitionId;
    }

    private TenantCertDefinition requireDefinition(Long tenantCertDefinitionId) {
        if (tenantCertDefinitionId == null) {
            throw new WorkflowValidationException("tenantCertDefinitionId is required");
        }
        return tenantDefinitionRepository
                .findById(tenantCertDefinitionId)
                .orElseThrow(
                        () ->
                                new WorkflowValidationException(
                                        "Certification definition not found: "
                                                + tenantCertDefinitionId));
    }

    private String resolveCertificationDefinitionName(Workflow workflow) {
        CertificationRecordWorkflowPayload payload =
                readPayloadIfPresent(workflow, CertificationRecordWorkflowPayload.class);
        if (payload == null || payload.getTenantCertDefinitionId() == null) {
            return null;
        }
        return tenantDefinitionRepository
                .findById(payload.getTenantCertDefinitionId())
                .map(
                        definition ->
                                StringUtils.hasText(definition.getName())
                                        ? definition.getName().trim()
                                        : null)
                .orElse(null);
    }
}
