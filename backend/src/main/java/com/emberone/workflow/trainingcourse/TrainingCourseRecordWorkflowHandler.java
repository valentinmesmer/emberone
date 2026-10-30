/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.trainingcourse;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Handles {@link WorkflowType#TRAINING_COURSE_RECORD}: a user submits a training course proof. On
 * approval the proof document is promoted to a {@code UserDocument} and a {@code
 * TrainingCourseUserSubmission} is created with the granted types.
 */
@Slf4j
@Component
public class TrainingCourseRecordWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<TrainingCourseRecordWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.trainingCourseRecord";

    private final WorkflowDocumentService workflowDocumentService;
    private final TrainingCourseRecordService trainingCourseRecordService;
    private final TrainingCourseCatalogService catalogService;
    private final TenantTrainingCourseTypeRepository tenantTypeRepository;

    public TrainingCourseRecordWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            TrainingCourseRecordService trainingCourseRecordService,
            TrainingCourseCatalogService catalogService,
            TenantTrainingCourseTypeRepository tenantTypeRepository,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.trainingCourseRecordService = trainingCourseRecordService;
        this.catalogService = catalogService;
        this.tenantTypeRepository = tenantTypeRepository;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.TRAINING_COURSE_RECORD;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<TrainingCourseRecordWorkflowPayload> ctx, Workflow existing) {
        TrainingCourseRecordWorkflowPayload payload = ctx.getPayload();
        if (payload == null) {
            return List.of();
        }
        return normalizeTypeIds(payload.getTenantTrainingCourseTypeIds()).stream()
                .map(WorkflowOpenConcurrencyKeys::trainingCourseType)
                .toList();
    }

    @Override
    public String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_TRAINING_COURSE_TYPE_ALREADY_OPEN;
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open training course submission for one of these types; "
                + "finish or cancel it before opening another";
    }

    @Override
    public Class<TrainingCourseRecordWorkflowPayload> payloadType() {
        return TrainingCourseRecordWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - training course submission is not possible";
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        String typeName = resolveTrainingCourseTypeNames(workflow);
        if (StringUtils.hasText(typeName)) {
            return documentPromotionMetadataSupport.fromInterpolatedTitle(
                    workflow, DOCUMENT_PROMOTION_PREFIX, Map.of("name", typeName.trim()));
        }
        return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                workflow, DOCUMENT_PROMOTION_PREFIX);
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return requireMembershipOrganization(openedByUserId);
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<TrainingCourseRecordWorkflowPayload> ctx) {
        TrainingCourseRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        for (Long typeId : normalizeTypeIds(payload.getTenantTrainingCourseTypeIds())) {
            requireType(typeId);
        }
        return requireMembershipOrganization(ctx.getOpenedByUserId());
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        TrainingCourseRecordWorkflowPayload payload =
                readPayloadIfPresent(workflow, TrainingCourseRecordWorkflowPayload.class);
        if (payload != null) {
            return resolveContextOrganization(
                    new WorkflowOpenContext<>(
                            workflow.getOpenedByUserId(), workflow.getTenantId(), payload));
        }
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    @Override
    public void validateOpen(WorkflowOpenContext<TrainingCourseRecordWorkflowPayload> ctx) {
        TrainingCourseRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        List<Long> typeIds = normalizeTypeIds(payload.getTenantTrainingCourseTypeIds());
        if (typeIds.isEmpty()) {
            throw new WorkflowValidationException("tenantTrainingCourseTypeIds is required");
        }
        if (typeIds.size() > TrainingCourseRecordWorkflowPayload.MAX_GRANTED_TYPES) {
            throw new WorkflowValidationException(
                    "At most "
                            + TrainingCourseRecordWorkflowPayload.MAX_GRANTED_TYPES
                            + " training course types may be granted");
        }
        if (payload.getStartDate() == null) {
            throw new WorkflowValidationException("startDate is required");
        }
        if (payload.getEndDate() == null) {
            throw new WorkflowValidationException("endDate is required");
        }
        if (!StringUtils.hasText(payload.getLocation())) {
            throw new WorkflowValidationException("location is required");
        }
        if (payload.getStartDate().isAfter(payload.getEndDate())) {
            throw new WorkflowValidationException("startDate must be on or before endDate");
        }
        if (typeIds.size() > 1 && !StringUtils.hasText(payload.getName())) {
            throw new WorkflowValidationException(
                    "name is required when multiple types are granted");
        }
        if (StringUtils.hasText(payload.getName())
                && payload.getName().trim().length()
                        > TrainingCourseRecordWorkflowPayload.MAX_NAME_LENGTH) {
            throw new WorkflowValidationException("name is too long");
        }
        requireOpeningUser(ctx);
        Long membershipOrgId = requireMembershipOrganization(ctx.getOpenedByUserId());
        for (Long typeId : typeIds) {
            TenantTrainingCourseType type = requireType(typeId);
            requireCatalogSelectableTrainingType(type);
            try {
                catalogService.requireTypeAvailableForOrg(typeId, membershipOrgId);
            } catch (RuntimeException ex) {
                throw new WorkflowValidationException(ex.getMessage());
            }
        }
        validateWorkflowDraftAttachments(
                ctx.getOpenedByUserId(),
                membershipOrgId,
                payload.getAttachmentIds(),
                TrainingCourseRecordWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<TrainingCourseRecordWorkflowPayload> ctx, Workflow existing) {
        TrainingCourseRecordWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        List<Long> typeIds = normalizeTypeIds(payload.getTenantTrainingCourseTypeIds());
        if (typeIds.isEmpty()) {
            throw new WorkflowValidationException("tenantTrainingCourseTypeIds is required");
        }
        requireOpeningUser(ctx);
        Long membershipOrgId = requireMembershipOrganization(ctx.getOpenedByUserId());
        for (Long typeId : typeIds) {
            TenantTrainingCourseType type = requireType(typeId);
            requireCatalogSelectableTrainingType(type);
            try {
                catalogService.requireTypeAvailableForOrg(typeId, membershipOrgId);
            } catch (RuntimeException ex) {
                throw new WorkflowValidationException(ex.getMessage());
            }
        }
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                membershipOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                TrainingCourseRecordWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachTrainingCourseAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachTrainingCourseAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        TrainingCourseRecordWorkflowPayload payload =
                readPayload(workflow, TrainingCourseRecordWorkflowPayload.class);
        trainingCourseRecordService.createSubmissionFromWorkflow(
                workflow.getTenantId(),
                workflow.getOpenedByUserId(),
                payload.getName(),
                payload.getTenantTrainingCourseTypeIds(),
                payload.getStartDate(),
                payload.getEndDate(),
                payload.getLocation(),
                payload.getHours(),
                payload.getNotes(),
                workflow.getId());
        log.info(
                "Created training course record(s) for user {} via approved workflow {}",
                workflow.getOpenedByUserId(),
                workflow.getId());
    }

    @Override
    public void onDocumentsPromoted(Workflow workflow, List<Long> documentIds) {
        trainingCourseRecordService.linkDocumentsFromWorkflow(
                workflow.getTenantId(), workflow.getId(), documentIds);
    }

    private TenantTrainingCourseType requireType(Long tenantTrainingCourseTypeId) {
        if (tenantTrainingCourseTypeId == null) {
            throw new WorkflowValidationException("tenantTrainingCourseTypeIds is required");
        }
        return tenantTypeRepository
                .findById(tenantTrainingCourseTypeId)
                .orElseThrow(
                        () ->
                                new WorkflowValidationException(
                                        "Training course type not found: "
                                                + tenantTrainingCourseTypeId));
    }

    private List<Long> normalizeTypeIds(List<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long typeId : typeIds) {
            if (typeId != null) {
                unique.add(typeId);
            }
        }
        return List.copyOf(unique);
    }

    private String resolveTrainingCourseTypeNames(Workflow workflow) {
        TrainingCourseRecordWorkflowPayload payload =
                readPayloadIfPresent(workflow, TrainingCourseRecordWorkflowPayload.class);
        if (payload == null) {
            return null;
        }
        if (StringUtils.hasText(payload.getName())) {
            return payload.getName().trim();
        }
        List<String> names =
                normalizeTypeIds(payload.getTenantTrainingCourseTypeIds()).stream()
                        .map(typeId -> tenantTypeRepository.findById(typeId))
                        .flatMap(Optional::stream)
                        .map(TenantTrainingCourseType::getName)
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .toList();
        if (names.isEmpty()) {
            return null;
        }
        return names.stream().collect(Collectors.joining(", "));
    }

    private static void requireCatalogSelectableTrainingType(TenantTrainingCourseType type) {
        if (!LifecycleFields.isCatalogSelectable(type)) {
            throw new WorkflowValidationException(
                    type.isArchived()
                            ? "Training course type is archived"
                            : "Training course type is not active");
        }
    }
}
