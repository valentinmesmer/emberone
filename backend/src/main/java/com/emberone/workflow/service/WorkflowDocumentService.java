/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.security.TokenService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowComment;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.drivinglicense.DrivingLicenseRecordWorkflowPayload;
import com.emberone.workflow.dto.AddressChangeWorkflowPayload;
import com.emberone.workflow.dto.CertificationRecordWorkflowPayload;
import com.emberone.workflow.dto.MainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.dto.NameChangeWorkflowPayload;
import com.emberone.workflow.dto.OrganizationGroupJoinRequestWorkflowPayload;
import com.emberone.workflow.dto.OrganizationGroupLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.OrganizationJoinRequestWorkflowPayload;
import com.emberone.workflow.dto.OrganizationLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import com.emberone.workflow.dto.UserDocumentSubmissionWorkflowPayload;
import com.emberone.workflow.exception.WorkflowNotFoundException;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.rankrequest.RankRequestWorkflowPayload;
import com.emberone.workflow.repository.WorkflowCommentRepository;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Workflow supporting documents in a single system-wide workflows bucket. Object keys are scoped by
 * tenant and workflow: {@code tenants/{tenantId}/workflows/{workflowId}/…}; drafts before open use
 * {@code tenants/{tenantId}/workflows/drafts/users/{userId}/…}.
 */
@Service
@RequiredArgsConstructor
public class WorkflowDocumentService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowCommentRepository workflowCommentRepository;
    private final EntityAttachmentRepository entityAttachmentRepository;
    private final EntityAttachmentService entityAttachmentService;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final TokenService tokenService;
    private final JsonMapper jsonMapper;

    @Value("${object-storage.workflows-bucket}")
    private String workflowsBucket;

    private static String draftStorageKey(
            Long tenantId, Long userId, String documentKind, String extension) {
        return "tenants/"
                + tenantId
                + "/workflows/drafts/users/"
                + userId
                + "/"
                + documentKind
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    private static String workflowStorageKey(Long tenantId, Long workflowId, String extension) {
        return "tenants/"
                + tenantId
                + "/workflows/"
                + workflowId
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    private static String commentDraftStorageKey(
            Long tenantId, Long workflowId, Long userId, String extension) {
        return "tenants/"
                + tenantId
                + "/workflows/"
                + workflowId
                + "/comment-drafts/users/"
                + userId
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    private static String commentStorageKey(
            Long tenantId, Long workflowId, Long commentId, String extension) {
        return "tenants/"
                + tenantId
                + "/workflows/"
                + workflowId
                + "/comments/"
                + commentId
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    private static String documentKindFor(WorkflowType workflowType) {
        return switch (workflowType) {
            case NAME_CHANGE -> "name-change";
            case ADDRESS_CHANGE -> "address-change";
            case CERTIFICATION_RECORD -> "certification-record";
            case TRAINING_COURSE_RECORD -> "training-course-record";
            case DRIVING_LICENSE_RECORD -> "driving-license-record";
            case ORGANIZATION_JOIN_REQUEST -> "organization-join-request";
            case MAIN_ORGANIZATION_CHANGE -> "main-organization-change";
            case ORGANIZATION_LEAVE_REQUEST -> "organization-leave-request";
            case ORGANIZATION_GROUP_LEAVE_REQUEST -> "organization-group-leave-request";
            case ORGANIZATION_GROUP_JOIN_REQUEST -> "organization-group-join-request";
            case ONBOARDING_SUBMISSION -> "onboarding-submission";
            case ORGANIZATION_MEMBERSHIP_END_REQUEST -> "organization-membership-end-request";
            case ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE ->
                    "organization-user-main-organization-change";
            case USER_DOCUMENT_SUBMISSION -> "user-document-submission";
            case RANK_REQUEST -> "rank-request";
            case COURSE_OFFERING_REGISTRATION -> "course-offering-registration";
            case COURSE_REGISTRATION_CANCELLATION -> "course-registration-cancellation";
        };
    }

    @Transactional
    public EntityAttachmentDto uploadWorkflowAttachmentDraft(
            Long userId, long workflowTypeId, MultipartFile file) {
        WorkflowType workflowType = WorkflowType.fromId(workflowTypeId);
        Long organizationId = resolveDraftOrganization(userId, workflowTypeId);
        try {
            return uploadWorkflowAttachmentDraft(
                    userId, organizationId, file, documentKindFor(workflowType));
        } catch (IllegalArgumentException ex) {
            throw new WorkflowValidationException(ex.getMessage());
        }
    }

    /**
     * Moves name-change attachment drafts into the workflow-scoped path and links them to the
     * workflow instance.
     */
    @Transactional
    public void attachNameChangeAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.NAME_CHANGE.getId())) {
            return;
        }
        NameChangeWorkflowPayload payload = readNameChangePayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new WorkflowValidationException("At least one attachment is required");
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /** Moves user-document-submission attachment drafts into the workflow-scoped path. */
    @Transactional
    public void attachUserDocumentSubmissionAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.USER_DOCUMENT_SUBMISSION.getId())) {
            return;
        }
        UserDocumentSubmissionWorkflowPayload payload = readUserDocumentSubmissionPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new WorkflowValidationException("At least one attachment is required");
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /** Moves address-change attachment drafts into the workflow-scoped path when present. */
    @Transactional
    public void attachAddressChangeAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.ADDRESS_CHANGE.getId())) {
            return;
        }
        AddressChangeWorkflowPayload payload = readAddressChangePayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves certification-record proof attachment drafts into the workflow-scoped path when
     * present. Requiredness is enforced in {@code
     * CertificationRecordWorkflowHandler.validateOpen}/{@code validateUpdate}.
     */
    @Transactional
    public void attachCertificationAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.CERTIFICATION_RECORD.getId())) {
            return;
        }
        CertificationRecordWorkflowPayload payload = readCertificationPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Removes process copies of a certification workflow (proof, comment attachments and comment
     * drafts) from the workflows bucket after promotion or when no personal copy is kept.
     */
    @Transactional
    public void deleteCertificationProofAttachments(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.CERTIFICATION_RECORD.getId())) {
            return;
        }
        List<EntityAttachment> attachments =
                new ArrayList<>(
                        entityAttachmentRepository.findByEntityTypeAndEntityId(
                                EntityAttachmentType.WORKFLOW, workflow.getId()));
        attachments.addAll(
                entityAttachmentRepository.findByEntityTypeAndEntityId(
                        EntityAttachmentType.WORKFLOW_COMMENT_DRAFT, workflow.getId()));
        for (WorkflowComment comment :
                workflowCommentRepository.findByWorkflow_IdOrderByIdAsc(workflow.getId())) {
            attachments.addAll(
                    entityAttachmentRepository.findByEntityTypeAndEntityId(
                            EntityAttachmentType.WORKFLOW_COMMENT, comment.getId()));
        }
        attachments.forEach(
                attachment -> entityAttachmentService.delete(workflowsBucket, attachment));
    }

    /**
     * Moves training-course-record proof attachment drafts into the workflow-scoped path (at least
     * one required).
     */
    @Transactional
    public void attachTrainingCourseAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.TRAINING_COURSE_RECORD.getId())) {
            return;
        }
        TrainingCourseRecordWorkflowPayload payload = readTrainingCoursePayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new WorkflowValidationException("At least one attachment is required");
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves driving-license-record proof attachment drafts into the workflow-scoped path (at least
     * one required).
     */
    @Transactional
    public void attachDrivingLicenseAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.DRIVING_LICENSE_RECORD.getId())) {
            return;
        }
        DrivingLicenseRecordWorkflowPayload payload = readDrivingLicensePayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new WorkflowValidationException("At least one attachment is required");
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /** Optional proof documents for a first-rank request. */
    @Transactional
    public void attachRankRequestAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.RANK_REQUEST.getId())) {
            return;
        }
        RankRequestWorkflowPayload payload = readRankRequestPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves organization-join-request attachment drafts into the workflow-scoped path when present.
     */
    @Transactional
    public void attachOrganizationJoinRequestAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId())) {
            return;
        }
        OrganizationJoinRequestWorkflowPayload payload =
                readOrganizationJoinRequestPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves main-organization-change attachment drafts into the workflow-scoped path when present.
     */
    @Transactional
    public void attachMainOrganizationChangeAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.MAIN_ORGANIZATION_CHANGE.getId())) {
            return;
        }
        MainOrganizationChangeWorkflowPayload payload = readMainOrganizationChangePayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves organization-leave-request attachment drafts into the workflow-scoped path when
     * present.
     */
    @Transactional
    public void attachOrganizationLeaveRequestAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId().equals(WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId())) {
            return;
        }
        OrganizationLeaveRequestWorkflowPayload payload =
                readOrganizationLeaveRequestPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves organization-group-leave-request attachment drafts into the workflow-scoped path when
     * present.
     */
    @Transactional
    public void attachOrganizationGroupLeaveRequestAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId()
                .equals(WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId())) {
            return;
        }
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                readOrganizationGroupLeaveRequestPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    /**
     * Moves organization-group-join-request attachment drafts into the workflow-scoped path when
     * present.
     */
    @Transactional
    public void attachOrganizationGroupJoinRequestAttachmentsToWorkflow(Workflow workflow) {
        if (!workflow.getWorkflowTypeId()
                .equals(WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST.getId())) {
            return;
        }
        OrganizationGroupJoinRequestWorkflowPayload payload =
                readOrganizationGroupJoinRequestPayload(workflow);
        List<Long> attachmentIds = payload.getAttachmentIds();
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachDraftToWorkflow(workflow, attachmentId);
        }
    }

    @Transactional(readOnly = true)
    public List<EntityAttachmentDto> listWorkflowAttachments(Long workflowId) {
        Workflow workflow =
                workflowRepository
                        .findWithDetailsById(workflowId)
                        .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        List<EntityAttachment> linked =
                entityAttachmentService.findEntitiesByTypeAndId(
                        EntityAttachmentType.WORKFLOW, workflowId);
        if (!linked.isEmpty()) {
            return linked.stream().map(entityAttachmentService::toDto).toList();
        }

        List<Long> payloadAttachmentIds = payloadAttachmentIds(workflow);
        if (payloadAttachmentIds.isEmpty()) {
            return List.of();
        }

        List<EntityAttachment> fromPayload = new ArrayList<>();
        for (Long attachmentId : payloadAttachmentIds) {
            entityAttachmentRepository
                    .findById(attachmentId)
                    .filter(att -> isAccessibleWorkflowAttachment(workflow, att))
                    .ifPresent(fromPayload::add);
        }
        return entityAttachmentService.sortNewestFirst(fromPayload).stream()
                .map(entityAttachmentService::toDto)
                .toList();
    }

    @Transactional
    public EntityAttachmentDto uploadWorkflowCommentAttachmentDraft(
            Workflow workflow, Long userId, MultipartFile file) {
        validateWorkflowAttachmentFile(file);

        Long tenantId = workflow.getTenantId();
        String extension = EntityAttachmentService.getFileExtension(file.getOriginalFilename());
        String storageKey = commentDraftStorageKey(tenantId, workflow.getId(), userId, extension);
        Long organizationId = resolveDraftOrganization(userId, workflow.getWorkflowTypeId());

        EntityAttachment attachment =
                entityAttachmentService.uploadNew(
                        workflowsBucket,
                        tenantId,
                        userId,
                        organizationId,
                        EntityAttachmentType.WORKFLOW_COMMENT_DRAFT,
                        workflow.getId(),
                        file,
                        storageKey);
        return entityAttachmentService.toDto(attachment);
    }

    @Transactional
    public EntityAttachmentDto attachCommentDraftToComment(
            Workflow workflow, WorkflowComment comment, Long attachmentId, Long userId) {
        EntityAttachment attachment =
                entityAttachmentRepository
                        .findById(attachmentId)
                        .filter(
                                att ->
                                        att.getEntityType()
                                                == EntityAttachmentType.WORKFLOW_COMMENT_DRAFT)
                        .filter(att -> att.getEntityId().equals(workflow.getId()))
                        .filter(att -> att.getUploadedBy().equals(userId))
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Comment attachment not found or not owned by you"));

        String extension = EntityAttachmentService.getFileExtension(attachment.getFileName());
        String storageKey =
                commentStorageKey(
                        workflow.getTenantId(), workflow.getId(), comment.getId(), extension);
        entityAttachmentService.relink(
                workflowsBucket,
                attachment,
                storageKey,
                EntityAttachmentType.WORKFLOW_COMMENT,
                comment.getId());
        return entityAttachmentService.toDto(attachment);
    }

    @Transactional(readOnly = true)
    public Map<Long, EntityAttachmentDto> listCommentAttachments(
            Workflow workflow, List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EntityAttachmentDto> result = new HashMap<>();
        for (Long commentId : commentIds) {
            entityAttachmentRepository
                    .findByEntityTypeAndEntityId(EntityAttachmentType.WORKFLOW_COMMENT, commentId)
                    .stream()
                    .findFirst()
                    .map(entityAttachmentService::toDto)
                    .ifPresent(dto -> result.put(commentId, dto));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public EntityAttachmentService.DownloadBundle downloadWorkflowAttachment(
            Long workflowId, Long attachmentId) {
        EntityAttachment attachment = resolveWorkflowAttachment(workflowId, attachmentId);
        return entityAttachmentService.downloadBundle(workflowsBucket, attachment);
    }

    private EntityAttachmentDto uploadWorkflowAttachmentDraft(
            Long userId, Long organizationId, MultipartFile file, String documentKind) {
        validateWorkflowAttachmentFile(file);

        Long tenantId = requireTenantId();
        String extension = EntityAttachmentService.getFileExtension(file.getOriginalFilename());
        String storageKey = draftStorageKey(tenantId, userId, documentKind, extension);

        EntityAttachment attachment =
                entityAttachmentService.uploadNew(
                        workflowsBucket,
                        tenantId,
                        userId,
                        organizationId,
                        EntityAttachmentType.WORKFLOW_DRAFT,
                        userId,
                        file,
                        storageKey);
        return entityAttachmentService.toDto(attachment);
    }

    private void attachDraftToWorkflow(Workflow workflow, Long attachmentId) {
        EntityAttachment attachment =
                entityAttachmentRepository
                        .findById(attachmentId)
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Attachment not found or does not belong to your organization / account"));

        if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW
                && workflow.getId().equals(attachment.getEntityId())) {
            return;
        }

        if (attachment.getEntityType() != EntityAttachmentType.WORKFLOW_DRAFT
                || !attachment.getEntityId().equals(workflow.getOpenedByUserId())) {
            throw new WorkflowValidationException(
                    "Attachment not found or does not belong to your organization / account");
        }

        if (!attachment.getUploadedBy().equals(workflow.getOpenedByUserId())) {
            throw new WorkflowValidationException("Attachment was not uploaded by you");
        }

        String extension = EntityAttachmentService.getFileExtension(attachment.getFileName());
        String workflowKey =
                workflowStorageKey(workflow.getTenantId(), workflow.getId(), extension);
        entityAttachmentService.relink(
                workflowsBucket,
                attachment,
                workflowKey,
                EntityAttachmentType.WORKFLOW,
                workflow.getId());
    }

    private EntityAttachment resolveWorkflowAttachment(Long workflowId, Long attachmentId) {
        Workflow workflow =
                workflowRepository
                        .findWithDetailsById(workflowId)
                        .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        EntityAttachment attachment =
                entityAttachmentRepository
                        .findById(attachmentId)
                        .orElseThrow(() -> new WorkflowValidationException("Attachment not found"));

        if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW
                && attachment.getEntityId().equals(workflow.getId())) {
            return attachment;
        }

        if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW_COMMENT) {
            return resolveWorkflowCommentAttachment(workflow, attachment);
        }

        List<Long> payloadAttachmentIds = payloadAttachmentIds(workflow);
        if (payloadAttachmentIds.contains(attachmentId)) {
            validateWorkflowAttachmentAccess(workflow, attachment);
            return attachment;
        }

        throw new WorkflowValidationException("Attachment does not belong to this workflow");
    }

    private List<Long> payloadAttachmentIds(Workflow workflow) {
        if (workflow.getWorkflowTypeId().equals(WorkflowType.NAME_CHANGE.getId())) {
            NameChangeWorkflowPayload payload = readNameChangePayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.ADDRESS_CHANGE.getId())) {
            AddressChangeWorkflowPayload payload = readAddressChangePayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.CERTIFICATION_RECORD.getId())) {
            CertificationRecordWorkflowPayload payload = readCertificationPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.TRAINING_COURSE_RECORD.getId())) {
            TrainingCourseRecordWorkflowPayload payload = readTrainingCoursePayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.DRIVING_LICENSE_RECORD.getId())) {
            DrivingLicenseRecordWorkflowPayload payload = readDrivingLicensePayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId())) {
            OrganizationJoinRequestWorkflowPayload payload =
                    readOrganizationJoinRequestPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.MAIN_ORGANIZATION_CHANGE.getId())) {
            MainOrganizationChangeWorkflowPayload payload =
                    readMainOrganizationChangePayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId())) {
            OrganizationLeaveRequestWorkflowPayload payload =
                    readOrganizationLeaveRequestPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId()
                .equals(WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId())) {
            OrganizationGroupLeaveRequestWorkflowPayload payload =
                    readOrganizationGroupLeaveRequestPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId()
                .equals(WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST.getId())) {
            OrganizationGroupJoinRequestWorkflowPayload payload =
                    readOrganizationGroupJoinRequestPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.USER_DOCUMENT_SUBMISSION.getId())) {
            UserDocumentSubmissionWorkflowPayload payload =
                    readUserDocumentSubmissionPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        if (workflow.getWorkflowTypeId().equals(WorkflowType.RANK_REQUEST.getId())) {
            RankRequestWorkflowPayload payload = readRankRequestPayload(workflow);
            return payload.getAttachmentIds() != null ? payload.getAttachmentIds() : List.of();
        }
        return List.of();
    }

    private boolean isAccessibleWorkflowAttachment(Workflow workflow, EntityAttachment attachment) {
        try {
            validateWorkflowAttachmentAccess(workflow, attachment);
            return true;
        } catch (WorkflowValidationException ex) {
            return false;
        }
    }

    private void validateWorkflowAttachmentAccess(Workflow workflow, EntityAttachment attachment) {
        if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW
                && attachment.getEntityId().equals(workflow.getId())) {
            return;
        }
        if (attachment.getEntityType() == EntityAttachmentType.WORKFLOW_DRAFT
                && attachment.getEntityId().equals(workflow.getOpenedByUserId())
                && attachment.getUploadedBy().equals(workflow.getOpenedByUserId())) {
            if (workflow.getStatus() == WorkflowStatus.OPEN
                    || workflow.getStatus() == WorkflowStatus.REJECTED) {
                return;
            }
        }
        throw new WorkflowValidationException(
                "Attachment is not accessible for this workflow state");
    }

    private NameChangeWorkflowPayload readNameChangePayload(Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(workflow.getPayload(), NameChangeWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private AddressChangeWorkflowPayload readAddressChangePayload(Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(workflow.getPayload(), AddressChangeWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private CertificationRecordWorkflowPayload readCertificationPayload(Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), CertificationRecordWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private TrainingCourseRecordWorkflowPayload readTrainingCoursePayload(Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), TrainingCourseRecordWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private DrivingLicenseRecordWorkflowPayload readDrivingLicensePayload(Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), DrivingLicenseRecordWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private RankRequestWorkflowPayload readRankRequestPayload(Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(workflow.getPayload(), RankRequestWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private OrganizationJoinRequestWorkflowPayload readOrganizationJoinRequestPayload(
            Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), OrganizationJoinRequestWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private MainOrganizationChangeWorkflowPayload readMainOrganizationChangePayload(
            Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), MainOrganizationChangeWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private OrganizationLeaveRequestWorkflowPayload readOrganizationLeaveRequestPayload(
            Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), OrganizationLeaveRequestWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private OrganizationGroupLeaveRequestWorkflowPayload readOrganizationGroupLeaveRequestPayload(
            Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), OrganizationGroupLeaveRequestWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private OrganizationGroupJoinRequestWorkflowPayload readOrganizationGroupJoinRequestPayload(
            Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), OrganizationGroupJoinRequestWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private UserDocumentSubmissionWorkflowPayload readUserDocumentSubmissionPayload(
            Workflow workflow) {
        if (workflow.getPayload() == null || workflow.getPayload().isBlank()) {
            throw new WorkflowValidationException("Workflow payload missing");
        }
        try {
            return jsonMapper.readValue(
                    workflow.getPayload(), UserDocumentSubmissionWorkflowPayload.class);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Invalid workflow payload: " + e.getMessage());
        }
    }

    private EntityAttachment resolveWorkflowCommentAttachment(
            Workflow workflow, EntityAttachment attachment) {
        WorkflowComment comment =
                workflowCommentRepository
                        .findById(attachment.getEntityId())
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Comment attachment not found"));
        if (!comment.getWorkflow().getId().equals(workflow.getId())) {
            throw new WorkflowValidationException("Attachment does not belong to this workflow");
        }
        return attachment;
    }

    private Long resolveDraftOrganization(Long userId, Long workflowTypeId) {
        Instant now = Instant.now();
        Optional<Long> membershipOrganizationId =
                membershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, now)
                        .or(
                                () ->
                                        membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(
                                                userId, now))
                        .map(membership -> membership.getOrganization().getId());
        if (membershipOrganizationId.isPresent()) {
            return membershipOrganizationId.get();
        }
        if (workflowTypeId != null
                && workflowTypeId == WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()) {
            return workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId).stream()
                    .filter(WorkflowDefinition::isEnabled)
                    .map(WorkflowDefinition::getOrganizationId)
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new WorkflowValidationException(
                                            "No organization membership - workflow attachment upload is not possible"));
        }
        throw new WorkflowValidationException(
                "No organization membership - workflow attachment upload is not possible");
    }

    private void validateWorkflowAttachmentFile(MultipartFile file) {
        try {
            EntityAttachmentService.validateFile(file);
            EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);
        } catch (IllegalArgumentException ex) {
            throw new WorkflowValidationException(ex.getMessage());
        }
    }

    private Long requireTenantId() {
        Long tenantId = tokenService.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId found in token");
        }
        return tenantId;
    }
}
