/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.certification.dto.OrgCertificationCatalogDto;
import com.emberone.certification.service.CertificationCatalogService;
import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.onboarding.config.EntryDocumentsStepConfig;
import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.domain.OnboardingInvitationWorkflowSubjects;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.dto.OnboardingDocumentCatalogItemDto;
import com.emberone.onboarding.dto.OnboardingInvitationCatalogDto;
import com.emberone.onboarding.dto.OnboardingInvitationStepDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.handler.OrganizationEntryDocumentsStepHandler;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentDefinitionService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.UserEntryDocumentFulfillmentService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowInputChange;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.repository.WorkflowRepository;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OnboardingInvitationProgressService {

    private final OnboardingInvitationRepository invitationRepository;
    private final OnboardingInvitationStepDataRepository stepDataRepository;
    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final StandardOnboardingStepService standardOnboardingStepService;
    private final OnboardingStepHandlerRegistry handlerRegistry;
    private final TenantOnboardingDocumentDefinitionRepository documentDefinitionRepository;
    private final OrganizationOnboardingDocumentDefinitionRepository
            organizationDocumentDefinitionRepository;
    private final OrganizationOnboardingDocumentDefinitionService
            organizationDocumentDefinitionService;
    private final OrganizationOnboardingDocumentTemplateService organizationDocumentTemplateService;
    private final OrganizationEntryDocumentsStepHandler organizationEntryDocumentsStepHandler;
    private final OnboardingDocumentTemplateService documentTemplateService;
    private final TrainingCourseCatalogService trainingCourseCatalogService;
    private final CertificationCatalogService certificationCatalogService;
    private final DrivingLicenseCatalogService drivingLicenseCatalogService;
    private final com.emberone.rank.service.TenantRankCatalogService tenantRankCatalogService;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;
    private final OnboardingEntryDocumentReuseService entryDocumentReuseService;
    private final OnboardingEntryDocumentDraftSupport entryDocumentDraftSupport;
    private final UserEntryDocumentFulfillmentService entryDocumentFulfillmentService;
    private final OnboardingInvitationReadinessService readinessService;
    private final WorkflowRepository workflowRepository;
    private final UserOrganizationMembershipRepository membershipRepository;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    @Transactional(readOnly = true)
    public SelfOnboardingInvitationDetailDto getDetail(long userId, long invitationId) {
        return toDetail(requireOwnInvitation(userId, invitationId));
    }

    @Transactional(readOnly = true)
    public SelfOnboardingInvitationDetailDto getDetailForOrganization(
            long organizationId, long invitationId) {
        return toDetail(requireInvitationForOrganization(organizationId, invitationId));
    }

    @Transactional(readOnly = true)
    public boolean isReadyToSubmit(long userId, long invitationId) {
        return readinessService.isReadyToSubmit(userId, invitationId);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto saveStep(
            long userId,
            long invitationId,
            OnboardingStepType stepType,
            Map<String, Object> payload) {
        return saveStep(requireEditableOwnInvitation(userId, invitationId), stepType, payload);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto saveStepForOrganization(
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            Map<String, Object> payload) {
        return saveStep(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                stepType,
                payload);
    }

    private SelfOnboardingInvitationDetailDto saveStep(
            OnboardingInvitation invitation,
            OnboardingStepType stepType,
            Map<String, Object> payload) {
        long invitationId = invitation.getId();
        StandardOnboardingStep step = requireConfiguredStep(invitation, stepType);
        OnboardingStepHandler handler = handlerRegistry.require(stepType);
        OnboardingStepConfig config = handler.parseConfig(step.getConfig());
        Map<String, Object> incoming = payload == null ? Map.of() : payload;
        handler.validateOnboardingPayload(config, incoming);
        Map<String, Object> normalized =
                new HashMap<>(handler.normalizeOnboardingPayload(config, incoming));
        normalized = ensureRecordKeys(stepType, normalized);
        Map<String, Object> previousPayload =
                stepDataRepository
                        .findByInvitationIdAndStepType(invitationId, stepType)
                        .map(OnboardingInvitationStepData::getPayload)
                        .orElse(null);
        if (stepType == OnboardingStepType.ENTRY_DOCUMENTS) {
            normalized =
                    mergeEntryDocumentFiles(
                            invitationId, OnboardingStepType.ENTRY_DOCUMENTS, normalized);
            normalized =
                    entryDocumentDraftSupport.finalizeTenantEntryDocumentPayload(
                            normalized, previousPayload);
            normalized = entryDocumentDraftSupport.stampTenantDocumentVersions(normalized);
        } else if (stepType == OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS) {
            OnboardingStepFulfillmentContext context =
                    new OnboardingStepFulfillmentContext(
                            invitation.getTenantId(),
                            invitation.getUserId(),
                            invitation.getTargetOrganizationId(),
                            invitationId);
            normalized =
                    new HashMap<>(
                            organizationEntryDocumentsStepHandler.normalizeOnboardingPayload(
                                    context, config, incoming));
            normalized =
                    mergeEntryDocumentFiles(
                            invitationId,
                            OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS,
                            normalized);
            normalized =
                    entryDocumentDraftSupport.finalizeOrganizationEntryDocumentPayload(
                            invitation.getTargetOrganizationId(), normalized, previousPayload);
            normalized =
                    entryDocumentDraftSupport.stampOrganizationDocumentVersions(
                            invitation.getTargetOrganizationId(), normalized);
        } else if (stepType == OnboardingStepType.CERTIFICATIONS
                || stepType == OnboardingStepType.TRAINING_COURSES) {
            normalized = mergeRecordAttachments(invitationId, stepType, normalized);
        } else if (stepType == OnboardingStepType.DRIVING_LICENSE) {
            normalized = mergeDrivingLicenseAttachment(invitationId, normalized);
        } else if (stepType == OnboardingStepType.RANK) {
            normalized = mergeRankAttachment(invitationId, normalized);
        }
        validateCatalogReferences(stepType, normalized, invitation.getTargetOrganizationId());
        validateAttachments(stepType, normalized, invitation.getTargetOrganizationId());

        OnboardingInvitationStepData data =
                stepDataRepository
                        .findByInvitationIdAndStepType(invitationId, stepType)
                        .orElseGet(
                                () ->
                                        OnboardingInvitationStepData.builder()
                                                .tenantId(invitation.getTenantId())
                                                .invitationId(invitationId)
                                                .stepType(stepType)
                                                .build());
        data.setPayload(normalized);
        stepDataRepository.save(data);
        cleanupOrphanedRecordAttachments(invitationId, stepType, normalized);
        return toDetail(invitation);
    }

    @Transactional
    public OnboardingDocumentTemplateService.TemplateDownload downloadTemplate(
            long userId, long invitationId, String configKey) {
        OnboardingInvitation invitation = requireOwnInvitation(userId, invitationId);
        markEntryDocumentTemplateOpened(invitation, OnboardingStepType.ENTRY_DOCUMENTS, configKey);
        return downloadTemplate(invitation, configKey);
    }

    @Transactional
    public OnboardingDocumentTemplateService.TemplateDownload downloadTemplateForOrganization(
            long organizationId, long invitationId, String configKey) {
        OnboardingInvitation invitation =
                requireInvitationForOrganization(organizationId, invitationId);
        markEntryDocumentTemplateOpened(invitation, OnboardingStepType.ENTRY_DOCUMENTS, configKey);
        return downloadTemplate(invitation, configKey);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadSignedDocument(long userId, long invitationId, String configKey) {
        return downloadSignedDocument(requireOwnInvitation(userId, invitationId), configKey);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadSignedDocumentForOrganization(
            long organizationId, long invitationId, String configKey) {
        return downloadSignedDocument(
                requireInvitationForOrganization(organizationId, invitationId), configKey);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadRecordAttachment(
            long userId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        return downloadRecordAttachment(
                requireOwnInvitation(userId, invitationId), stepType, recordKey, attachmentIndex);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadRecordAttachmentForOrganization(
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        return downloadRecordAttachment(
                requireInvitationForOrganization(organizationId, invitationId),
                stepType,
                recordKey,
                attachmentIndex);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadDrivingLicenseAttachment(
            long userId, long invitationId, int attachmentIndex) {
        return downloadDrivingLicenseAttachment(
                requireOwnInvitation(userId, invitationId), attachmentIndex);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadDrivingLicenseAttachmentForOrganization(
            long organizationId, long invitationId, int attachmentIndex) {
        return downloadDrivingLicenseAttachment(
                requireInvitationForOrganization(organizationId, invitationId), attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadRankAttachment(
            long userId, long invitationId, MultipartFile file) {
        return uploadRankAttachment(requireEditableOwnInvitation(userId, invitationId), file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadRankAttachmentForOrganization(
            long organizationId, long invitationId, MultipartFile file) {
        return uploadRankAttachment(
                requireEditableInvitationForOrganization(organizationId, invitationId), file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteRankAttachment(
            long userId, long invitationId, int attachmentIndex) {
        return deleteRankAttachment(
                requireEditableOwnInvitation(userId, invitationId), attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteRankAttachmentForOrganization(
            long organizationId, long invitationId, int attachmentIndex) {
        return deleteRankAttachment(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                attachmentIndex);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadRankAttachment(
            long userId, long invitationId, int attachmentIndex) {
        return downloadRankAttachment(requireOwnInvitation(userId, invitationId), attachmentIndex);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadRankAttachmentForOrganization(
            long organizationId, long invitationId, int attachmentIndex) {
        return downloadRankAttachment(
                requireInvitationForOrganization(organizationId, invitationId), attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadSignedDocument(
            long userId, long invitationId, String configKey, MultipartFile file) {
        return uploadSignedDocument(
                requireEditableOwnInvitation(userId, invitationId), configKey, file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadSignedDocumentForOrganization(
            long organizationId, long invitationId, String configKey, MultipartFile file) {
        return uploadSignedDocument(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                configKey,
                file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadOrganizationSignedDocument(
            long userId, long invitationId, String configKey, MultipartFile file) {
        return uploadSignedDocument(
                requireEditableOwnInvitation(userId, invitationId),
                OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS,
                configKey,
                file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadOrganizationSignedDocumentForOrganization(
            long organizationId, long invitationId, String configKey, MultipartFile file) {
        return uploadSignedDocument(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS,
                configKey,
                file);
    }

    private SelfOnboardingInvitationDetailDto uploadSignedDocument(
            OnboardingInvitation invitation, String configKey, MultipartFile file) {
        return uploadSignedDocument(
                invitation, OnboardingStepType.ENTRY_DOCUMENTS, configKey, file);
    }

    private SelfOnboardingInvitationDetailDto uploadSignedDocument(
            OnboardingInvitation invitation,
            OnboardingStepType stepType,
            String configKey,
            MultipartFile file) {
        long invitationId = invitation.getId();
        requireDocumentKey(invitation, stepType, configKey);
        int documentVersion;
        if (stepType == OnboardingStepType.ENTRY_DOCUMENTS) {
            TenantOnboardingDocumentDefinition definition =
                    documentDefinitionRepository
                            .findByConfigKey(configKey)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Onboarding document not found: " + configKey));
            if (definition.getFulfillmentMode()
                    != OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD) {
                throw new ConflictException(
                        "Document does not require a signed upload: " + configKey);
            }
            documentVersion = definition.getVersion();
        } else {
            OrganizationOnboardingDocumentDefinition definition =
                    organizationDocumentDefinitionRepository
                            .findByOrganizationIdAndConfigKey(
                                    invitation.getTargetOrganizationId(), configKey)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Onboarding document not found: " + configKey));
            if (definition.getFulfillmentMode()
                    != OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD) {
                throw new ConflictException(
                        "Document does not require a signed upload: " + configKey);
            }
            documentVersion = definition.getVersion();
        }
        EntityAttachmentService.validateFile(file);
        EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);

        OnboardingInvitationStepData data =
                stepDataRepository
                        .findByInvitationIdAndStepType(invitationId, stepType)
                        .orElseGet(
                                () ->
                                        OnboardingInvitationStepData.builder()
                                                .tenantId(invitation.getTenantId())
                                                .invitationId(invitationId)
                                                .stepType(stepType)
                                                .payload(OnboardingStepPayloads.emptyPayload())
                                                .build());
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        List<Map<String, Object>> documents =
                new ArrayList<>(
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.DOCUMENTS));
        Map<String, Object> current = findDocument(documents, configKey);
        deleteStoredUpload(current);

        String storageKey =
                uploadDraftAttachment(
                        invitation,
                        invitationId,
                        "steps/" + stepType.name() + "/documents/" + configKey,
                        file);

        Map<String, Object> next =
                current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
        next.put(OnboardingStepPayloads.CONFIG_KEY, configKey);
        next.put(OnboardingStepPayloads.FILE_NAME, file.getOriginalFilename());
        next.put(OnboardingStepPayloads.STORAGE_KEY, storageKey);
        next.put(
                OnboardingStepPayloads.CONTENT_TYPE,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        next.put(OnboardingStepPayloads.SIZE_BYTES, file.getSize());
        next.put(OnboardingStepPayloads.DOCUMENT_VERSION, documentVersion);
        next.put(OnboardingStepPayloads.CONFIRMED_AT, Instant.now().toString());
        if (stepType == OnboardingStepType.ENTRY_DOCUMENTS) {
            TenantOnboardingDocumentDefinition uploadDefinition =
                    documentDefinitionRepository
                            .findByConfigKey(configKey)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Onboarding document not found: " + configKey));
            entryDocumentDraftSupport.requireTemplateOpenedBeforeSignedUpload(
                    uploadDefinition, next);
        } else {
            OrganizationOnboardingDocumentDefinition uploadDefinition =
                    organizationDocumentDefinitionRepository
                            .findByOrganizationIdAndConfigKey(
                                    invitation.getTargetOrganizationId(), configKey)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Onboarding document not found: " + configKey));
            entryDocumentDraftSupport.requireTemplateOpenedBeforeSignedUpload(
                    uploadDefinition, next);
        }
        replaceDocument(documents, next);
        payload.put(OnboardingStepPayloads.DOCUMENTS, documents);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadRecordAttachment(
            long userId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            MultipartFile file) {
        return uploadRecordAttachment(
                requireEditableOwnInvitation(userId, invitationId), stepType, recordKey, file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadRecordAttachmentForOrganization(
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            MultipartFile file) {
        return uploadRecordAttachment(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                stepType,
                recordKey,
                file);
    }

    private SelfOnboardingInvitationDetailDto uploadRecordAttachment(
            OnboardingInvitation invitation,
            OnboardingStepType stepType,
            String recordKey,
            MultipartFile file) {
        long invitationId = invitation.getId();
        if (stepType != OnboardingStepType.CERTIFICATIONS
                && stepType != OnboardingStepType.TRAINING_COURSES) {
            throw new ConflictException(
                    "Record attachments are not supported for step: " + stepType);
        }
        if (recordKey == null || recordKey.isBlank()) {
            throw new ConflictException("Record key is required");
        }
        requireConfiguredStep(invitation, stepType);
        EntityAttachmentService.validateFile(file);
        EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);

        OnboardingInvitationStepData data = requireOrCreateStepData(invitation, stepType);
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        List<Map<String, Object>> records =
                new ArrayList<>(
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.RECORDS));
        Map<String, Object> current = findRecord(records, recordKey);

        String storageKey =
                uploadDraftAttachment(
                        invitation,
                        invitationId,
                        "steps/" + stepType.name() + "/records/" + recordKey,
                        file);

        Map<String, Object> attachment = buildAttachmentMap(file, storageKey);

        Map<String, Object> next =
                current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
        next.put(OnboardingStepPayloads.RECORD_KEY, recordKey);
        putAttachments(next, appendAttachment(next, attachment));
        replaceRecord(records, next);
        payload.put(OnboardingStepPayloads.RECORDS, records);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadDrivingLicenseAttachment(
            long userId, long invitationId, MultipartFile file) {
        return uploadDrivingLicenseAttachment(
                requireEditableOwnInvitation(userId, invitationId), file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadDrivingLicenseAttachmentForOrganization(
            long organizationId, long invitationId, MultipartFile file) {
        return uploadDrivingLicenseAttachment(
                requireEditableInvitationForOrganization(organizationId, invitationId), file);
    }

    private SelfOnboardingInvitationDetailDto uploadDrivingLicenseAttachment(
            OnboardingInvitation invitation, MultipartFile file) {
        long invitationId = invitation.getId();
        requireConfiguredStep(invitation, OnboardingStepType.DRIVING_LICENSE);
        EntityAttachmentService.validateFile(file);
        EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);

        OnboardingInvitationStepData data =
                requireOrCreateStepData(invitation, OnboardingStepType.DRIVING_LICENSE);
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        Map<String, Object> next =
                license == null ? new LinkedHashMap<>() : new LinkedHashMap<>(license);

        String storageKey =
                uploadDraftAttachment(invitation, invitationId, "steps/DRIVING_LICENSE", file);

        Map<String, Object> attachment = buildAttachmentMap(file, storageKey);
        putAttachments(next, appendAttachment(next, attachment));
        payload.put(OnboardingStepPayloads.LICENSE, next);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    private SelfOnboardingInvitationDetailDto uploadRankAttachment(
            OnboardingInvitation invitation, MultipartFile file) {
        long invitationId = invitation.getId();
        requireConfiguredStep(invitation, OnboardingStepType.RANK);
        EntityAttachmentService.validateFile(file);
        EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);

        OnboardingInvitationStepData data =
                requireOrCreateStepData(invitation, OnboardingStepType.RANK);
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.RANK);
        Map<String, Object> next = rank == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rank);

        String storageKey = uploadDraftAttachment(invitation, invitationId, "steps/RANK", file);
        Map<String, Object> attachment = buildAttachmentMap(file, storageKey);
        putAttachments(next, List.of(attachment));
        payload.put(OnboardingStepPayloads.RANK, next);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteRecordAttachment(
            long userId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        return deleteRecordAttachment(
                requireEditableOwnInvitation(userId, invitationId),
                stepType,
                recordKey,
                attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteRecordAttachmentForOrganization(
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        return deleteRecordAttachment(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                stepType,
                recordKey,
                attachmentIndex);
    }

    private SelfOnboardingInvitationDetailDto deleteRecordAttachment(
            OnboardingInvitation invitation,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        if (stepType != OnboardingStepType.CERTIFICATIONS
                && stepType != OnboardingStepType.TRAINING_COURSES) {
            throw new ConflictException(
                    "Record attachments are not supported for step: " + stepType);
        }
        if (recordKey == null || recordKey.isBlank()) {
            throw new ConflictException("Record key is required");
        }
        requireConfiguredStep(invitation, stepType);

        OnboardingInvitationStepData data = requireOrCreateStepData(invitation, stepType);
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        List<Map<String, Object>> records =
                new ArrayList<>(
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.RECORDS));
        Map<String, Object> current = findRecord(records, recordKey);
        if (current == null) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        Map<String, Object> next = new LinkedHashMap<>(current);
        putAttachments(next, removeAttachmentAt(next, attachmentIndex));
        replaceRecord(records, next);
        payload.put(OnboardingStepPayloads.RECORDS, records);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteDrivingLicenseAttachment(
            long userId, long invitationId, int attachmentIndex) {
        return deleteDrivingLicenseAttachment(
                requireEditableOwnInvitation(userId, invitationId), attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteDrivingLicenseAttachmentForOrganization(
            long organizationId, long invitationId, int attachmentIndex) {
        return deleteDrivingLicenseAttachment(
                requireEditableInvitationForOrganization(organizationId, invitationId),
                attachmentIndex);
    }

    private SelfOnboardingInvitationDetailDto deleteDrivingLicenseAttachment(
            OnboardingInvitation invitation, int attachmentIndex) {
        requireConfiguredStep(invitation, OnboardingStepType.DRIVING_LICENSE);

        OnboardingInvitationStepData data =
                requireOrCreateStepData(invitation, OnboardingStepType.DRIVING_LICENSE);
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        if (license == null) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        Map<String, Object> next = new LinkedHashMap<>(license);
        putAttachments(next, removeAttachmentAt(next, attachmentIndex));
        payload.put(OnboardingStepPayloads.LICENSE, next);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    private SelfOnboardingInvitationDetailDto deleteRankAttachment(
            OnboardingInvitation invitation, int attachmentIndex) {
        requireConfiguredStep(invitation, OnboardingStepType.RANK);

        OnboardingInvitationStepData data =
                requireOrCreateStepData(invitation, OnboardingStepType.RANK);
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.RANK);
        if (rank == null) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        Map<String, Object> next = new LinkedHashMap<>(rank);
        putAttachments(next, removeAttachmentAt(next, attachmentIndex));
        payload.put(OnboardingStepPayloads.RANK, next);
        data.setPayload(payload);
        stepDataRepository.save(data);
        return toDetail(invitation);
    }

    private SelfOnboardingInvitationDetailDto toDetail(OnboardingInvitation invitation) {
        Organization organization =
                organizationRepository
                        .findById(invitation.getTargetOrganizationId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: "
                                                        + invitation.getTargetOrganizationId()));
        List<StandardOnboardingStep> configuredSteps =
                effectiveStandardResolver
                        .resolveForOrganization(organization)
                        .map(standardOnboardingStepService::listSteps)
                        .orElse(List.of());
        Map<OnboardingStepType, OnboardingInvitationStepData> dataByType =
                stepDataRepository.findByInvitationId(invitation.getId()).stream()
                        .collect(
                                Collectors.toMap(
                                        OnboardingInvitationStepData::getStepType,
                                        Function.identity()));

        if (invitation.getStatus() == OnboardingInvitationStatus.APPROVED) {
            return toApprovedDetail(invitation, organization, configuredSteps, dataByType);
        }

        OnboardingStepFulfillmentContext context =
                new OnboardingStepFulfillmentContext(
                        invitation.getTenantId(),
                        invitation.getUserId(),
                        invitation.getTargetOrganizationId(),
                        invitation.getId());

        boolean allFulfilled =
                readinessService.areAllStepsFulfilled(configuredSteps, dataByType, context);
        List<OnboardingStepType> alreadyFulfilledStepTypes = new ArrayList<>();
        List<OnboardingInvitationStepDto> steps = new ArrayList<>();
        for (StandardOnboardingStep step : configuredSteps) {
            OnboardingStepHandler handler = handlerRegistry.require(step.getStepType());
            OnboardingStepConfig config = handler.parseConfig(step.getConfig());
            OnboardingInvitationStepData data = dataByType.get(step.getStepType());
            Map<String, Object> payload = data == null ? Map.of() : data.getPayload();
            boolean liveFulfilled = handler.isFulfilled(context, config);
            if (liveFulfilled && !handler.hasInvitationWork(config, payload)) {
                alreadyFulfilledStepTypes.add(step.getStepType());
                continue;
            }
            boolean fulfilled =
                    liveFulfilled || handler.isOnboardingFulfilled(context, config, payload);
            steps.add(
                    OnboardingInvitationStepDto.builder()
                            .stepType(step.getStepType())
                            .sortOrder(step.getSortOrder())
                            .config(step.getConfig() == null ? Map.of() : step.getConfig())
                            .fulfilled(fulfilled)
                            .payload(publicPayload(step.getStepType(), payload))
                            .build());
        }

        return detailDto(
                invitation,
                organization,
                steps,
                alreadyFulfilledStepTypes,
                allFulfilled,
                invitation.getStatus() == OnboardingInvitationStatus.OPEN && allFulfilled,
                buildCatalog(organization, configuredSteps, context));
    }

    /**
     * Approved invitations are frozen to invitation drafts: later standard/catalog changes must not
     * surface as missing steps or documents.
     */
    private SelfOnboardingInvitationDetailDto toApprovedDetail(
            OnboardingInvitation invitation,
            Organization organization,
            List<StandardOnboardingStep> configuredSteps,
            Map<OnboardingStepType, OnboardingInvitationStepData> dataByType) {
        Map<OnboardingStepType, StandardOnboardingStep> configuredByType =
                configuredSteps.stream()
                        .collect(
                                Collectors.toMap(
                                        StandardOnboardingStep::getStepType,
                                        Function.identity(),
                                        (left, right) -> left,
                                        LinkedHashMap::new));

        LinkedHashSet<OnboardingStepType> candidates = new LinkedHashSet<>();
        candidates.addAll(configuredByType.keySet());
        candidates.addAll(dataByType.keySet());

        List<OnboardingInvitationStepDto> steps = new ArrayList<>();
        for (OnboardingStepType stepType : candidates) {
            OnboardingStepHandler handler = handlerRegistry.require(stepType);
            StandardOnboardingStep configured = configuredByType.get(stepType);
            OnboardingStepConfig config =
                    handler.parseConfig(rawConfigForApproved(stepType, configured));
            OnboardingInvitationStepData data = dataByType.get(stepType);
            Map<String, Object> payload = data == null ? Map.of() : data.getPayload();
            if (!handler.hasInvitationWork(config, payload)) {
                continue;
            }
            int sortOrder =
                    configured != null ? configured.getSortOrder() : 1_000 + stepType.ordinal();
            Map<String, Object> configMap =
                    configured != null && configured.getConfig() != null
                            ? configured.getConfig()
                            : Map.of();
            steps.add(
                    OnboardingInvitationStepDto.builder()
                            .stepType(stepType)
                            .sortOrder(sortOrder)
                            .config(configMap)
                            .fulfilled(true)
                            .payload(publicPayload(stepType, payload))
                            .build());
        }
        steps.sort(Comparator.comparingInt(OnboardingInvitationStepDto::getSortOrder));

        return detailDto(
                invitation,
                organization,
                steps,
                List.of(),
                true,
                false,
                buildApprovedCatalog(invitation, organization, dataByType));
    }

    private SelfOnboardingInvitationDetailDto detailDto(
            OnboardingInvitation invitation,
            Organization organization,
            List<OnboardingInvitationStepDto> steps,
            List<OnboardingStepType> alreadyFulfilledStepTypes,
            boolean allStepsFulfilled,
            boolean completionEnabled,
            OnboardingInvitationCatalogDto catalog) {
        Optional<Workflow> awaitingChange = findAwaitingInputChangeSubmission(invitation);
        return SelfOnboardingInvitationDetailDto.builder()
                .id(invitation.getId())
                .subjectUserId(invitation.getUserId())
                .targetOrganizationId(organization.getId())
                .targetOrganizationName(organization.getName())
                .status(invitation.getStatus())
                .invitedAt(invitation.getInvitedAt())
                .editable(isEditable(invitation))
                .allStepsFulfilled(allStepsFulfilled)
                .completionEnabled(completionEnabled)
                .alreadyFulfilledStepTypes(List.copyOf(alreadyFulfilledStepTypes))
                .inputChangeMessage(
                        awaitingChange
                                .flatMap(Workflow::findOpenInputChange)
                                .map(WorkflowInputChange::getMessage)
                                .orElse(null))
                .steps(steps)
                .catalog(catalog)
                .build();
    }

    private static Map<String, Object> rawConfigForApproved(
            OnboardingStepType stepType, StandardOnboardingStep configured) {
        if (configured != null && configured.getConfig() != null) {
            return configured.getConfig();
        }
        // Removed from the live standard: keep hasInvitationWork meaningful for empty drafts.
        if (stepType == OnboardingStepType.EMERGENCY_CONTACTS) {
            return Map.of("minimumCount", 1);
        }
        if (stepType == OnboardingStepType.ENTRY_DOCUMENTS) {
            return Map.of("documentConfigKeys", List.of());
        }
        return Map.of();
    }

    private boolean isEditable(OnboardingInvitation invitation) {
        return invitation.getStatus() == OnboardingInvitationStatus.OPEN;
    }

    private Optional<Workflow> findAwaitingInputChangeSubmission(OnboardingInvitation invitation) {
        return workflowRepository
                .findActiveBySubject(
                        invitation.getUserId(),
                        WorkflowType.ONBOARDING_SUBMISSION.getId(),
                        OnboardingInvitationWorkflowSubjects.SUBMISSION_ENTITY_TYPE,
                        invitation.getId())
                .filter(workflow -> workflow.getStatus() == WorkflowStatus.AWAITING_INPUT_CHANGE);
    }

    private OnboardingInvitationCatalogDto buildCatalog(
            Organization organization,
            List<StandardOnboardingStep> steps,
            OnboardingStepFulfillmentContext context) {
        List<String> documentKeys = List.of();
        boolean hasOrganizationDocumentsStep = false;
        for (StandardOnboardingStep step : steps) {
            if (step.getStepType() == OnboardingStepType.ENTRY_DOCUMENTS) {
                OnboardingStepConfig parsed =
                        handlerRegistry
                                .require(OnboardingStepType.ENTRY_DOCUMENTS)
                                .parseConfig(step.getConfig());
                if (parsed instanceof EntryDocumentsStepConfig entryConfig) {
                    documentKeys = entryConfig.documentConfigKeys();
                }
            } else if (step.getStepType() == OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS) {
                hasOrganizationDocumentsStep = true;
            }
        }
        List<OnboardingDocumentCatalogItemDto> documents = new ArrayList<>();
        Set<String> reusableDocumentKeys =
                entryDocumentReuseService.findReusableTenantConfigKeys(context, documentKeys);
        for (String configKey : documentKeys) {
            documentDefinitionRepository
                    .findByConfigKey(configKey)
                    .filter(LifecycleFields::isCatalogSelectable)
                    .ifPresent(
                            definition ->
                                    documents.add(
                                            toCatalogItem(
                                                    definition.getConfigKey(),
                                                    definition.getName(),
                                                    definition.getDescription(),
                                                    definition.getFulfillmentMode(),
                                                    definition.isMandatory(),
                                                    documentTemplateService
                                                            .findCurrentTemplateMetadata(
                                                                    definition),
                                                    reusableDocumentKeys.contains(
                                                            definition.getConfigKey()),
                                                    null)));
        }
        List<OnboardingDocumentCatalogItemDto> organizationDocuments = new ArrayList<>();
        if (hasOrganizationDocumentsStep) {
            List<String> organizationDocumentKeys =
                    organizationDocumentDefinitionService.listActiveConfigKeys(
                            organization.getId());
            Set<String> reusableOrganizationDocumentKeys =
                    entryDocumentReuseService.findReusableOrganizationConfigKeys(
                            context, organizationDocumentKeys);
            for (String configKey : organizationDocumentKeys) {
                organizationDocumentDefinitionRepository
                        .findByOrganizationIdAndConfigKey(organization.getId(), configKey)
                        .filter(LifecycleFields::isCatalogSelectable)
                        .ifPresent(
                                definition ->
                                        organizationDocuments.add(
                                                toCatalogItem(
                                                        definition.getConfigKey(),
                                                        definition.getName(),
                                                        definition.getDescription(),
                                                        definition.getFulfillmentMode(),
                                                        definition.isMandatory(),
                                                        organizationDocumentTemplateService
                                                                .findCurrentTemplateMetadata(
                                                                        definition),
                                                        reusableOrganizationDocumentKeys.contains(
                                                                definition.getConfigKey()),
                                                        null)));
            }
        }
        return catalogWithLookups(organization, steps, documents, organizationDocuments);
    }

    private OnboardingInvitationCatalogDto buildApprovedCatalog(
            OnboardingInvitation invitation,
            Organization organization,
            Map<OnboardingStepType, OnboardingInvitationStepData> dataByType) {
        List<OnboardingDocumentCatalogItemDto> documents =
                catalogItemsFromDraftDocuments(
                        invitation,
                        organization,
                        dataByType.get(OnboardingStepType.ENTRY_DOCUMENTS),
                        true);
        List<OnboardingDocumentCatalogItemDto> organizationDocuments =
                catalogItemsFromDraftDocuments(
                        invitation,
                        organization,
                        dataByType.get(OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS),
                        false);
        return catalogWithLookups(organization, List.of(), documents, organizationDocuments);
    }

    private List<OnboardingDocumentCatalogItemDto> catalogItemsFromDraftDocuments(
            OnboardingInvitation invitation,
            Organization organization,
            OnboardingInvitationStepData stepData,
            boolean tenantScoped) {
        if (stepData == null) {
            return List.of();
        }
        List<String> configKeys = new ArrayList<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(
                        stepData.getPayload(), OnboardingStepPayloads.DOCUMENTS)) {
            if (!draftDocumentHasWork(document)) {
                continue;
            }
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (!configKey.isBlank()) {
                configKeys.add(configKey);
            }
        }
        Map<String, Long> promotedUserDocumentIds =
                tenantScoped
                        ? entryDocumentFulfillmentService.findTenantPromotedUserDocumentIds(
                                invitation.getUserId(), configKeys)
                        : entryDocumentFulfillmentService.findOrganizationPromotedUserDocumentIds(
                                organization.getId(), invitation.getUserId(), configKeys);
        List<OnboardingDocumentCatalogItemDto> items = new ArrayList<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(
                        stepData.getPayload(), OnboardingStepPayloads.DOCUMENTS)) {
            if (!draftDocumentHasWork(document)) {
                continue;
            }
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (configKey.isBlank()) {
                continue;
            }
            Long promotedUserDocumentId = promotedUserDocumentIds.get(configKey);
            if (tenantScoped) {
                Optional<TenantOnboardingDocumentDefinition> definition =
                        documentDefinitionRepository.findByConfigKey(configKey);
                items.add(
                        definition
                                .map(
                                        def ->
                                                toCatalogItem(
                                                        def.getConfigKey(),
                                                        def.getName(),
                                                        def.getDescription(),
                                                        def.getFulfillmentMode(),
                                                        def.isMandatory(),
                                                        documentTemplateService
                                                                .findCurrentTemplateMetadata(def),
                                                        false,
                                                        promotedUserDocumentId))
                                .orElseGet(
                                        () ->
                                                fallbackCatalogItem(
                                                        configKey, promotedUserDocumentId)));
            } else {
                Optional<OrganizationOnboardingDocumentDefinition> definition =
                        organizationDocumentDefinitionRepository.findByOrganizationIdAndConfigKey(
                                organization.getId(), configKey);
                items.add(
                        definition
                                .map(
                                        def ->
                                                toCatalogItem(
                                                        def.getConfigKey(),
                                                        def.getName(),
                                                        def.getDescription(),
                                                        def.getFulfillmentMode(),
                                                        def.isMandatory(),
                                                        organizationDocumentTemplateService
                                                                .findCurrentTemplateMetadata(def),
                                                        false,
                                                        promotedUserDocumentId))
                                .orElseGet(
                                        () ->
                                                fallbackCatalogItem(
                                                        configKey, promotedUserDocumentId)));
            }
        }
        return items;
    }

    private static boolean draftDocumentHasWork(Map<String, Object> document) {
        return OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED)
                || OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED)
                || OnboardingStepPayloads.readNullableString(
                                document, OnboardingStepPayloads.STORAGE_KEY)
                        != null;
    }

    private static OnboardingDocumentCatalogItemDto fallbackCatalogItem(
            String configKey, Long promotedUserDocumentId) {
        return toCatalogItem(
                configKey,
                configKey,
                "",
                OnboardingDocumentFulfillmentMode.CONFIRMED,
                true,
                Optional.empty(),
                false,
                promotedUserDocumentId);
    }

    private OnboardingInvitationCatalogDto catalogWithLookups(
            Organization organization,
            List<StandardOnboardingStep> steps,
            List<OnboardingDocumentCatalogItemDto> documents,
            List<OnboardingDocumentCatalogItemDto> organizationDocuments) {
        List<TrainingCourseTypeDto> trainingTypes =
                trainingCourseCatalogService
                        .getAvailableTypesForOrg(organization.getId())
                        .getTypes();
        List<CertificationDefinitionDto> certifications =
                certificationCatalogService.getCatalog(organization.getId()).getDefinitions();
        List<DrivingLicenseDefinitionDto> licenseClasses =
                drivingLicenseCatalogService
                        .getAvailableDefinitionsForOrg(organization.getId())
                        .getDefinitions();
        boolean hasRankStep =
                steps.stream().anyMatch(step -> step.getStepType() == OnboardingStepType.RANK);
        List<com.emberone.rank.dto.TenantRankDto> ranks =
                hasRankStep
                        ? tenantRankCatalogService.listActiveRanks(organization.getTenantId())
                        : List.of();
        return OnboardingInvitationCatalogDto.builder()
                .documents(documents)
                .organizationDocuments(organizationDocuments)
                .trainingCourseTypes(trainingTypes)
                .certifications(certifications)
                .drivingLicenseClasses(licenseClasses)
                .ranks(ranks)
                .build();
    }

    private static OnboardingDocumentCatalogItemDto toCatalogItem(
            String configKey,
            String name,
            String description,
            OnboardingDocumentFulfillmentMode fulfillmentMode,
            boolean mandatory,
            Optional<OnboardingDocumentTemplateService.TemplateMetadata> templateMetadata,
            boolean alreadyFulfilled,
            Long promotedUserDocumentId) {
        return OnboardingDocumentCatalogItemDto.builder()
                .configKey(configKey)
                .name(name)
                .description(description)
                .fulfillmentMode(fulfillmentMode)
                .mandatory(mandatory)
                .hasTemplate(templateMetadata.isPresent())
                .templateFileName(
                        templateMetadata
                                .map(OnboardingDocumentTemplateService.TemplateMetadata::fileName)
                                .orElse(null))
                .templateVersion(
                        templateMetadata
                                .map(OnboardingDocumentTemplateService.TemplateMetadata::version)
                                .orElse(null))
                .templateUploadedAt(
                        templateMetadata
                                .map(OnboardingDocumentTemplateService.TemplateMetadata::uploadedAt)
                                .orElse(null))
                .alreadyFulfilled(alreadyFulfilled)
                .promotedUserDocumentId(promotedUserDocumentId)
                .build();
    }

    private void validateCatalogReferences(
            OnboardingStepType stepType, Map<String, Object> payload, long organizationId) {
        switch (stepType) {
            case TRAINING_COURSES -> {
                for (Map<String, Object> record :
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.RECORDS)) {
                    for (Long typeId :
                            OnboardingStepPayloads.readLongList(
                                    record,
                                    OnboardingStepPayloads.TENANT_TRAINING_COURSE_TYPE_IDS)) {
                        trainingCourseCatalogService.requireTypeAvailableForOrg(
                                typeId, organizationId);
                    }
                }
            }
            case CERTIFICATIONS -> {
                for (Map<String, Object> record :
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.RECORDS)) {
                    Long definitionId =
                            OnboardingStepPayloads.readLong(
                                    record, OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID);
                    if (definitionId != null) {
                        certificationCatalogService.requireTypeAvailableForOrg(
                                definitionId, organizationId);
                    }
                }
            }
            case RANK -> {
                if (OnboardingStepPayloads.readBoolean(
                        payload, OnboardingStepPayloads.NONE_DECLARED)) {
                    return;
                }
                Long tenantRankId =
                        OnboardingStepPayloads.readLong(
                                payload, OnboardingStepPayloads.TENANT_RANK_ID);
                if (tenantRankId != null) {
                    tenantRankCatalogService.requireActiveRank(tenantRankId);
                }
            }
            case DRIVING_LICENSE -> {
                Map<String, Object> license =
                        OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
                if (license == null) {
                    return;
                }
                for (Map<String, Object> licenseClass :
                        OnboardingStepPayloads.readObjectList(
                                license, OnboardingStepPayloads.CLASSES)) {
                    Long definitionId =
                            OnboardingStepPayloads.readLong(
                                    licenseClass,
                                    OnboardingStepPayloads.TENANT_DRIVING_LICENSE_DEFINITION_ID);
                    if (definitionId != null) {
                        drivingLicenseCatalogService.requireDefinitionAvailableForOrg(
                                definitionId, organizationId);
                    }
                }
            }
            default -> {}
        }
    }

    private Map<String, Object> mergeEntryDocumentFiles(
            long invitationId, OnboardingStepType stepType, Map<String, Object> incoming) {
        Map<String, Map<String, Object>> existingByKey = new LinkedHashMap<>();
        stepDataRepository
                .findByInvitationIdAndStepType(invitationId, stepType)
                .ifPresent(
                        data -> {
                            for (Map<String, Object> document :
                                    OnboardingStepPayloads.readObjectList(
                                            data.getPayload(), OnboardingStepPayloads.DOCUMENTS)) {
                                String configKey =
                                        OnboardingStepPayloads.readString(
                                                document, OnboardingStepPayloads.CONFIG_KEY);
                                if (!configKey.isBlank()) {
                                    existingByKey.put(configKey, document);
                                }
                            }
                        });
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(incoming, OnboardingStepPayloads.DOCUMENTS)) {
            Map<String, Object> next = new LinkedHashMap<>(document);
            next.remove(OnboardingStepPayloads.STORAGE_KEY);
            next.remove(OnboardingStepPayloads.CONTENT_TYPE);
            next.remove(OnboardingStepPayloads.SIZE_BYTES);
            next.remove(OnboardingStepPayloads.FILE_NAME);
            next.remove(OnboardingStepPayloads.UPLOADED);
            String configKey =
                    OnboardingStepPayloads.readString(next, OnboardingStepPayloads.CONFIG_KEY);
            Map<String, Object> existing = existingByKey.get(configKey);
            if (existing != null) {
                copyIfPresent(existing, next, OnboardingStepPayloads.STORAGE_KEY);
                copyIfPresent(existing, next, OnboardingStepPayloads.CONTENT_TYPE);
                copyIfPresent(existing, next, OnboardingStepPayloads.SIZE_BYTES);
                copyIfPresent(existing, next, OnboardingStepPayloads.FILE_NAME);
                copyIfPresent(existing, next, OnboardingStepPayloads.OPENED_AT);
                if (!OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)) {
                    copyIfPresent(existing, next, OnboardingStepPayloads.CONFIRMATION_TEXT);
                    copyIfPresent(existing, next, OnboardingStepPayloads.CONFIRMED_AT);
                }
            }
            merged.add(next);
        }
        Map<String, Object> result = new LinkedHashMap<>(incoming);
        result.put(OnboardingStepPayloads.DOCUMENTS, merged);
        return result;
    }

    private Map<String, Object> mergeRecordAttachments(
            long invitationId, OnboardingStepType stepType, Map<String, Object> incoming) {
        Map<String, Map<String, Object>> existingByKey = new LinkedHashMap<>();
        stepDataRepository
                .findByInvitationIdAndStepType(invitationId, stepType)
                .ifPresent(
                        data -> {
                            for (Map<String, Object> record :
                                    OnboardingStepPayloads.readObjectList(
                                            data.getPayload(), OnboardingStepPayloads.RECORDS)) {
                                String recordKey =
                                        OnboardingStepPayloads.readString(
                                                record, OnboardingStepPayloads.RECORD_KEY);
                                if (!recordKey.isBlank()) {
                                    existingByKey.put(recordKey, record);
                                }
                            }
                        });
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(incoming, OnboardingStepPayloads.RECORDS)) {
            Map<String, Object> next = new LinkedHashMap<>(record);
            stripAttachmentFields(next);
            String recordKey =
                    OnboardingStepPayloads.readString(next, OnboardingStepPayloads.RECORD_KEY);
            Map<String, Object> existing = existingByKey.get(recordKey);
            if (existing != null) {
                putAttachments(next, OnboardingStepPayloads.readAttachments(existing));
            }
            merged.add(next);
        }
        Map<String, Object> result = new LinkedHashMap<>(incoming);
        result.put(OnboardingStepPayloads.RECORDS, merged);
        return result;
    }

    private Map<String, Object> mergeDrivingLicenseAttachment(
            long invitationId, Map<String, Object> incoming) {
        Map<String, Object> existingLicense = null;
        var existingData =
                stepDataRepository.findByInvitationIdAndStepType(
                        invitationId, OnboardingStepType.DRIVING_LICENSE);
        if (existingData.isPresent()) {
            existingLicense =
                    OnboardingStepPayloads.readObject(
                            existingData.get().getPayload(), OnboardingStepPayloads.LICENSE);
        }
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(incoming, OnboardingStepPayloads.LICENSE);
        if (license == null) {
            return incoming;
        }
        Map<String, Object> next = new LinkedHashMap<>(license);
        stripAttachmentFields(next);
        if (existingLicense != null) {
            putAttachments(next, OnboardingStepPayloads.readAttachments(existingLicense));
        }
        Map<String, Object> result = new LinkedHashMap<>(incoming);
        result.put(OnboardingStepPayloads.LICENSE, next);
        return result;
    }

    private Map<String, Object> mergeRankAttachment(
            long invitationId, Map<String, Object> incoming) {
        Map<String, Object> existingRank = null;
        var existingData =
                stepDataRepository.findByInvitationIdAndStepType(
                        invitationId, OnboardingStepType.RANK);
        if (existingData.isPresent()) {
            existingRank =
                    OnboardingStepPayloads.readObject(
                            existingData.get().getPayload(), OnboardingStepPayloads.RANK);
        }
        if (OnboardingStepPayloads.readBoolean(incoming, OnboardingStepPayloads.NONE_DECLARED)) {
            Map<String, Object> cleared = new LinkedHashMap<>(incoming);
            cleared.put(OnboardingStepPayloads.RANK, null);
            return cleared;
        }
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(incoming, OnboardingStepPayloads.RANK);
        Map<String, Object> next = rank == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rank);
        stripAttachmentFields(next);
        if (existingRank != null) {
            putAttachments(next, OnboardingStepPayloads.readAttachments(existingRank));
        }
        Map<String, Object> result = new LinkedHashMap<>(incoming);
        result.put(OnboardingStepPayloads.RANK, next);
        return result;
    }

    private void validateAttachments(
            OnboardingStepType stepType, Map<String, Object> payload, long organizationId) {
        switch (stepType) {
            case CERTIFICATIONS -> {
                OrgCertificationCatalogDto catalog =
                        certificationCatalogService.getCatalog(organizationId);
                Map<Long, CertificationDefinitionDto> definitions =
                        catalog.getDefinitions().stream()
                                .collect(
                                        Collectors.toMap(
                                                CertificationDefinitionDto::getId,
                                                Function.identity()));
                for (Map<String, Object> record :
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.RECORDS)) {
                    Long definitionId =
                            OnboardingStepPayloads.readLong(
                                    record, OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID);
                    if (definitionId == null) {
                        continue;
                    }
                    CertificationDefinitionDto definition = definitions.get(definitionId);
                    if (definition != null
                            && requiresDocumentEvidence(definition.getEvidenceTypes())
                            && !hasStoredAttachment(record)) {
                        throw new ConflictException(
                                "Certification record requires a document attachment");
                    }
                }
            }
            case TRAINING_COURSES -> {
                for (Map<String, Object> record :
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.RECORDS)) {
                    if (!hasStoredAttachment(record)) {
                        throw new ConflictException(
                                "Training course record requires a document attachment");
                    }
                }
            }
            case DRIVING_LICENSE -> {
                Map<String, Object> license =
                        OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
                if (license != null && !hasStoredAttachment(license)) {
                    throw new ConflictException("Driving license requires a document attachment");
                }
            }
            default -> {}
        }
    }

    private void cleanupOrphanedRecordAttachments(
            long invitationId, OnboardingStepType stepType, Map<String, Object> normalized) {
        if (stepType != OnboardingStepType.CERTIFICATIONS
                && stepType != OnboardingStepType.TRAINING_COURSES) {
            if (stepType == OnboardingStepType.DRIVING_LICENSE) {
                cleanupOrphanedDrivingLicenseAttachment(invitationId, normalized);
            } else if (stepType == OnboardingStepType.RANK) {
                cleanupOrphanedRankAttachment(invitationId, normalized);
            }
            return;
        }
        Set<String> retainedKeys = new HashSet<>();
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(normalized, OnboardingStepPayloads.RECORDS)) {
            String recordKey =
                    OnboardingStepPayloads.readString(record, OnboardingStepPayloads.RECORD_KEY);
            if (!recordKey.isBlank()) {
                retainedKeys.add(recordKey);
            }
        }
        stepDataRepository
                .findByInvitationIdAndStepType(invitationId, stepType)
                .ifPresent(
                        data -> {
                            for (Map<String, Object> record :
                                    OnboardingStepPayloads.readObjectList(
                                            data.getPayload(), OnboardingStepPayloads.RECORDS)) {
                                String recordKey =
                                        OnboardingStepPayloads.readString(
                                                record, OnboardingStepPayloads.RECORD_KEY);
                                if (!recordKey.isBlank() && !retainedKeys.contains(recordKey)) {
                                    deleteStoredUpload(record);
                                }
                            }
                        });
    }

    private void cleanupOrphanedDrivingLicenseAttachment(
            long invitationId, Map<String, Object> normalized) {
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(normalized, OnboardingStepPayloads.LICENSE);
        if (license != null) {
            return;
        }
        stepDataRepository
                .findByInvitationIdAndStepType(invitationId, OnboardingStepType.DRIVING_LICENSE)
                .ifPresent(
                        data -> {
                            Map<String, Object> existing =
                                    OnboardingStepPayloads.readObject(
                                            data.getPayload(), OnboardingStepPayloads.LICENSE);
                            deleteStoredUpload(existing);
                        });
    }

    private void cleanupOrphanedRankAttachment(long invitationId, Map<String, Object> normalized) {
        if (OnboardingStepPayloads.readBoolean(normalized, OnboardingStepPayloads.NONE_DECLARED)) {
            stepDataRepository
                    .findByInvitationIdAndStepType(invitationId, OnboardingStepType.RANK)
                    .ifPresent(
                            data -> {
                                Map<String, Object> existing =
                                        OnboardingStepPayloads.readObject(
                                                data.getPayload(), OnboardingStepPayloads.RANK);
                                deleteStoredUpload(existing);
                            });
            return;
        }
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(normalized, OnboardingStepPayloads.RANK);
        if (rank != null) {
            return;
        }
        stepDataRepository
                .findByInvitationIdAndStepType(invitationId, OnboardingStepType.RANK)
                .ifPresent(
                        data -> {
                            Map<String, Object> existing =
                                    OnboardingStepPayloads.readObject(
                                            data.getPayload(), OnboardingStepPayloads.RANK);
                            deleteStoredUpload(existing);
                        });
    }

    private OnboardingInvitationStepData requireOrCreateStepData(
            OnboardingInvitation invitation, OnboardingStepType stepType) {
        return stepDataRepository
                .findByInvitationIdAndStepType(invitation.getId(), stepType)
                .orElseGet(
                        () ->
                                OnboardingInvitationStepData.builder()
                                        .tenantId(invitation.getTenantId())
                                        .invitationId(invitation.getId())
                                        .stepType(stepType)
                                        .payload(OnboardingStepPayloads.emptyPayload())
                                        .build());
    }

    private static Map<String, Object> findRecord(
            List<Map<String, Object>> records, String recordKey) {
        for (Map<String, Object> record : records) {
            if (recordKey.equals(
                    OnboardingStepPayloads.readString(record, OnboardingStepPayloads.RECORD_KEY))) {
                return record;
            }
        }
        return null;
    }

    private static void replaceRecord(List<Map<String, Object>> records, Map<String, Object> next) {
        String recordKey =
                OnboardingStepPayloads.readString(next, OnboardingStepPayloads.RECORD_KEY);
        for (int i = 0; i < records.size(); i++) {
            if (recordKey.equals(
                    OnboardingStepPayloads.readString(
                            records.get(i), OnboardingStepPayloads.RECORD_KEY))) {
                records.set(i, next);
                return;
            }
        }
        records.add(next);
    }

    private static void stripAttachmentFields(Map<String, Object> map) {
        map.remove(OnboardingStepPayloads.ATTACHMENTS);
        map.remove(OnboardingStepPayloads.FILE_NAMES);
        map.remove(OnboardingStepPayloads.STORAGE_KEY);
        map.remove(OnboardingStepPayloads.CONTENT_TYPE);
        map.remove(OnboardingStepPayloads.SIZE_BYTES);
        map.remove(OnboardingStepPayloads.FILE_NAME);
        map.remove(OnboardingStepPayloads.UPLOADED);
    }

    private static boolean hasStoredAttachment(Map<String, Object> map) {
        return !OnboardingStepPayloads.readAttachments(map).isEmpty();
    }

    private static boolean requiresDocumentEvidence(Collection<String> evidenceTypes) {
        return evidenceTypes != null && evidenceTypes.contains("DOCUMENT");
    }

    private static Map<String, Object> ensureRecordKeys(
            OnboardingStepType stepType, Map<String, Object> payload) {
        if (stepType != OnboardingStepType.CERTIFICATIONS
                && stepType != OnboardingStepType.TRAINING_COURSES) {
            return payload;
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            Map<String, Object> next = new LinkedHashMap<>(record);
            String recordKey =
                    OnboardingStepPayloads.readNullableString(
                            next, OnboardingStepPayloads.RECORD_KEY);
            if (recordKey == null || recordKey.isBlank()) {
                next.put(OnboardingStepPayloads.RECORD_KEY, UUID.randomUUID().toString());
            }
            records.add(next);
        }
        Map<String, Object> result = new LinkedHashMap<>(payload);
        result.put(OnboardingStepPayloads.RECORDS, records);
        return result;
    }

    private static Map<String, Object> publicPayload(
            OnboardingStepType stepType, Map<String, Object> payload) {
        return switch (stepType) {
            case ENTRY_DOCUMENTS, ORGANIZATION_ENTRY_DOCUMENTS -> publishEntryDocuments(payload);
            case CERTIFICATIONS, TRAINING_COURSES -> publishRecordsWithAttachments(payload);
            case DRIVING_LICENSE -> publishDrivingLicenseWithAttachment(payload);
            default -> payload;
        };
    }

    private static Map<String, Object> publishEntryDocuments(Map<String, Object> payload) {
        List<Map<String, Object>> documents = new ArrayList<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            Map<String, Object> published = new LinkedHashMap<>();
            published.put(
                    OnboardingStepPayloads.CONFIG_KEY,
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY));
            published.put(
                    OnboardingStepPayloads.CONFIRMED,
                    OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED));
            published.put(
                    OnboardingStepPayloads.DECLINED,
                    OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED));
            String fileName =
                    OnboardingStepPayloads.readNullableString(
                            document, OnboardingStepPayloads.FILE_NAME);
            boolean uploaded =
                    OnboardingStepPayloads.readNullableString(
                                    document, OnboardingStepPayloads.STORAGE_KEY)
                            != null;
            published.put(OnboardingStepPayloads.FILE_NAME, fileName);
            published.put(OnboardingStepPayloads.UPLOADED, uploaded);
            published.put(
                    "templateOpened",
                    OnboardingStepPayloads.readInstant(document, OnboardingStepPayloads.OPENED_AT)
                            != null);
            documents.add(published);
        }
        Map<String, Object> published = new LinkedHashMap<>();
        published.put(OnboardingStepPayloads.DOCUMENTS, documents);
        return published;
    }

    private static Map<String, Object> publishAttachmentMetadata(Map<String, Object> stored) {
        Map<String, Object> published = new LinkedHashMap<>();
        List<String> fileNames = OnboardingStepPayloads.readAttachmentFileNames(stored);
        published.put(OnboardingStepPayloads.FILE_NAMES, fileNames);
        published.put(
                OnboardingStepPayloads.FILE_NAME, fileNames.isEmpty() ? null : fileNames.get(0));
        published.put(OnboardingStepPayloads.UPLOADED, !fileNames.isEmpty());
        return published;
    }

    private static Map<String, Object> publishRecordsWithAttachments(Map<String, Object> payload) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            Map<String, Object> published = new LinkedHashMap<>(record);
            published.remove(OnboardingStepPayloads.STORAGE_KEY);
            published.remove(OnboardingStepPayloads.CONTENT_TYPE);
            published.remove(OnboardingStepPayloads.SIZE_BYTES);
            published.remove(OnboardingStepPayloads.ATTACHMENTS);
            published.putAll(publishAttachmentMetadata(record));
            records.add(published);
        }
        Map<String, Object> published = new LinkedHashMap<>(payload);
        published.put(OnboardingStepPayloads.RECORDS, records);
        return published;
    }

    private static Map<String, Object> publishDrivingLicenseWithAttachment(
            Map<String, Object> payload) {
        Map<String, Object> published = new LinkedHashMap<>(payload);
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        if (license == null) {
            return published;
        }
        Map<String, Object> publishedLicense = new LinkedHashMap<>(license);
        publishedLicense.remove(OnboardingStepPayloads.STORAGE_KEY);
        publishedLicense.remove(OnboardingStepPayloads.CONTENT_TYPE);
        publishedLicense.remove(OnboardingStepPayloads.SIZE_BYTES);
        publishedLicense.remove(OnboardingStepPayloads.ATTACHMENTS);
        publishedLicense.putAll(publishAttachmentMetadata(license));
        published.put(OnboardingStepPayloads.LICENSE, publishedLicense);
        return published;
    }

    private String uploadDraftAttachment(
            OnboardingInvitation invitation,
            long invitationId,
            String pathSuffix,
            MultipartFile file) {
        String extension = EntityAttachmentService.getFileExtension(file.getOriginalFilename());
        String storageKey =
                "tenants/"
                        + invitation.getTenantId()
                        + "/onboarding-invitations/"
                        + invitationId
                        + "/"
                        + pathSuffix
                        + "/"
                        + UUID.randomUUID()
                        + extension;
        objectStorageTransactionCoordinator.uploadFileWithRollbackCleanup(
                file, attachmentsBucket, storageKey);
        return storageKey;
    }

    private static Map<String, Object> buildAttachmentMap(MultipartFile file, String storageKey) {
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put(OnboardingStepPayloads.FILE_NAME, file.getOriginalFilename());
        attachment.put(OnboardingStepPayloads.STORAGE_KEY, storageKey);
        attachment.put(
                OnboardingStepPayloads.CONTENT_TYPE,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.put(OnboardingStepPayloads.SIZE_BYTES, file.getSize());
        return attachment;
    }

    private static List<Map<String, Object>> appendAttachment(
            Map<String, Object> map, Map<String, Object> attachment) {
        List<Map<String, Object>> attachments =
                new ArrayList<>(OnboardingStepPayloads.readAttachments(map));
        if (attachments.size() >= OnboardingStepPayloads.MAX_ATTACHMENTS) {
            throw new ConflictException(
                    "Maximum number of attachments exceeded: "
                            + OnboardingStepPayloads.MAX_ATTACHMENTS);
        }
        attachments.add(attachment);
        return attachments;
    }

    private List<Map<String, Object>> removeAttachmentAt(
            Map<String, Object> stored, int attachmentIndex) {
        List<Map<String, Object>> attachments =
                new ArrayList<>(OnboardingStepPayloads.readAttachments(stored));
        if (attachmentIndex < 0 || attachmentIndex >= attachments.size()) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        deleteStoredAttachment(attachments.remove(attachmentIndex));
        return attachments;
    }

    private static void putAttachments(
            Map<String, Object> map, List<Map<String, Object>> attachments) {
        map.put(OnboardingStepPayloads.ATTACHMENTS, attachments);
        if (attachments.isEmpty()) {
            map.remove(OnboardingStepPayloads.FILE_NAME);
            map.remove(OnboardingStepPayloads.FILE_NAMES);
            map.remove(OnboardingStepPayloads.STORAGE_KEY);
            map.remove(OnboardingStepPayloads.CONTENT_TYPE);
            map.remove(OnboardingStepPayloads.SIZE_BYTES);
            map.remove(OnboardingStepPayloads.UPLOADED);
            return;
        }
        Map<String, Object> first = attachments.get(0);
        map.put(
                OnboardingStepPayloads.FILE_NAME,
                OnboardingStepPayloads.readNullableString(first, OnboardingStepPayloads.FILE_NAME));
        map.put(
                OnboardingStepPayloads.STORAGE_KEY,
                OnboardingStepPayloads.readNullableString(
                        first, OnboardingStepPayloads.STORAGE_KEY));
        map.put(
                OnboardingStepPayloads.CONTENT_TYPE,
                OnboardingStepPayloads.readNullableString(
                        first, OnboardingStepPayloads.CONTENT_TYPE));
        map.put(
                OnboardingStepPayloads.SIZE_BYTES,
                OnboardingStepPayloads.readLong(first, OnboardingStepPayloads.SIZE_BYTES));
        map.put(
                OnboardingStepPayloads.FILE_NAMES,
                attachments.stream()
                        .map(
                                attachment ->
                                        OnboardingStepPayloads.readNullableString(
                                                attachment, OnboardingStepPayloads.FILE_NAME))
                        .filter(fileName -> fileName != null && !fileName.isBlank())
                        .toList());
        map.put(OnboardingStepPayloads.UPLOADED, true);
    }

    private OnboardingInvitation requireOwnInvitation(long userId, long invitationId) {
        return invitationRepository
                .findByIdAndUserId(invitationId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding invitation not found: " + invitationId));
    }

    private OnboardingInvitation requireInvitationForOrganization(
            long organizationId, long invitationId) {
        return invitationRepository
                .findByIdAndTargetOrganizationId(invitationId, organizationId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding invitation not found: " + invitationId));
    }

    @Transactional
    public OrganizationOnboardingDocumentTemplateService.TemplateDownload
            downloadOrganizationTemplate(long userId, long invitationId, String configKey) {
        OnboardingInvitation invitation = requireOwnInvitation(userId, invitationId);
        markEntryDocumentTemplateOpened(
                invitation, OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS, configKey);
        return downloadOrganizationTemplate(invitation, configKey);
    }

    @Transactional
    public OrganizationOnboardingDocumentTemplateService.TemplateDownload
            downloadOrganizationTemplateForOrganization(
                    long organizationId, long invitationId, String configKey) {
        OnboardingInvitation invitation =
                requireInvitationForOrganization(organizationId, invitationId);
        markEntryDocumentTemplateOpened(
                invitation, OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS, configKey);
        return downloadOrganizationTemplate(invitation, configKey);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadOrganizationSignedDocument(
            long userId, long invitationId, String configKey) {
        return downloadOrganizationSignedDocument(
                requireOwnInvitation(userId, invitationId), configKey);
    }

    @Transactional(readOnly = true)
    public StoredUpload downloadOrganizationSignedDocumentForOrganization(
            long organizationId, long invitationId, String configKey) {
        return downloadOrganizationSignedDocument(
                requireInvitationForOrganization(organizationId, invitationId), configKey);
    }

    private OrganizationOnboardingDocumentTemplateService.TemplateDownload
            downloadOrganizationTemplate(OnboardingInvitation invitation, String configKey) {
        requireDocumentKey(invitation, OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS, configKey);
        return organizationDocumentTemplateService.downloadTemplate(
                invitation.getTenantId(), invitation.getTargetOrganizationId(), configKey);
    }

    private StoredUpload downloadOrganizationSignedDocument(
            OnboardingInvitation invitation, String configKey) {
        requireDocumentKey(invitation, OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS, configKey);
        Map<String, Object> document =
                findDocument(
                        OnboardingStepPayloads.readObjectList(
                                requireStepPayload(
                                        invitation,
                                        OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS),
                                OnboardingStepPayloads.DOCUMENTS),
                        configKey);
        return downloadStoredUpload(document, configKey, 0);
    }

    private void markEntryDocumentTemplateOpened(
            OnboardingInvitation invitation, OnboardingStepType stepType, String configKey) {
        if (!isEditable(invitation)) {
            return;
        }
        requireDocumentKey(invitation, stepType, configKey);
        long invitationId = invitation.getId();
        OnboardingInvitationStepData data =
                stepDataRepository
                        .findByInvitationIdAndStepType(invitationId, stepType)
                        .orElseGet(
                                () ->
                                        OnboardingInvitationStepData.builder()
                                                .tenantId(invitation.getTenantId())
                                                .invitationId(invitationId)
                                                .stepType(stepType)
                                                .payload(OnboardingStepPayloads.emptyPayload())
                                                .build());
        Map<String, Object> payload = new HashMap<>(data.getPayload());
        List<Map<String, Object>> documents =
                new ArrayList<>(
                        OnboardingStepPayloads.readObjectList(
                                payload, OnboardingStepPayloads.DOCUMENTS));
        Map<String, Object> document = findDocument(documents, configKey);
        Map<String, Object> next =
                document == null ? new LinkedHashMap<>() : new LinkedHashMap<>(document);
        next.put(OnboardingStepPayloads.CONFIG_KEY, configKey);
        if (document == null) {
            next.putIfAbsent(OnboardingStepPayloads.CONFIRMED, false);
            next.putIfAbsent(OnboardingStepPayloads.DECLINED, false);
        }
        if (OnboardingStepPayloads.readInstant(next, OnboardingStepPayloads.OPENED_AT) == null) {
            next.put(OnboardingStepPayloads.OPENED_AT, Instant.now().toString());
        }
        replaceDocument(documents, next);
        payload.put(OnboardingStepPayloads.DOCUMENTS, documents);
        data.setPayload(payload);
        stepDataRepository.save(data);
    }

    private OnboardingDocumentTemplateService.TemplateDownload downloadTemplate(
            OnboardingInvitation invitation, String configKey) {
        requireDocumentKey(invitation, OnboardingStepType.ENTRY_DOCUMENTS, configKey);
        return documentTemplateService.downloadTemplate(invitation.getTenantId(), configKey);
    }

    private StoredUpload downloadSignedDocument(OnboardingInvitation invitation, String configKey) {
        requireDocumentKey(invitation, OnboardingStepType.ENTRY_DOCUMENTS, configKey);
        Map<String, Object> document =
                findDocument(
                        OnboardingStepPayloads.readObjectList(
                                requireStepPayload(invitation, OnboardingStepType.ENTRY_DOCUMENTS),
                                OnboardingStepPayloads.DOCUMENTS),
                        configKey);
        return downloadStoredUpload(document, configKey, 0);
    }

    private StoredUpload downloadRecordAttachment(
            OnboardingInvitation invitation,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        if (stepType != OnboardingStepType.CERTIFICATIONS
                && stepType != OnboardingStepType.TRAINING_COURSES) {
            throw new ConflictException(
                    "Record attachments are not supported for step: " + stepType);
        }
        if (recordKey == null || recordKey.isBlank()) {
            throw new ConflictException("Record key is required");
        }
        Map<String, Object> record =
                findRecord(
                        OnboardingStepPayloads.readObjectList(
                                requireStepPayload(invitation, stepType),
                                OnboardingStepPayloads.RECORDS),
                        recordKey);
        return downloadStoredUpload(record, recordKey, attachmentIndex);
    }

    private StoredUpload downloadDrivingLicenseAttachment(
            OnboardingInvitation invitation, int attachmentIndex) {
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(
                        requireStepPayload(invitation, OnboardingStepType.DRIVING_LICENSE),
                        OnboardingStepPayloads.LICENSE);
        return downloadStoredUpload(license, "driving-license", attachmentIndex);
    }

    private StoredUpload downloadRankAttachment(
            OnboardingInvitation invitation, int attachmentIndex) {
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(
                        requireStepPayload(invitation, OnboardingStepType.RANK),
                        OnboardingStepPayloads.RANK);
        return downloadStoredUpload(rank, "rank-proof", attachmentIndex);
    }

    private Map<String, Object> requireStepPayload(
            OnboardingInvitation invitation, OnboardingStepType stepType) {
        requireConfiguredStep(invitation, stepType);
        return stepDataRepository
                .findByInvitationIdAndStepType(invitation.getId(), stepType)
                .map(OnboardingInvitationStepData::getPayload)
                .orElse(Map.of());
    }

    private StoredUpload downloadStoredUpload(
            Map<String, Object> stored, String fallbackFileName, int attachmentIndex) {
        List<Map<String, Object>> attachments = OnboardingStepPayloads.readAttachments(stored);
        if (attachments.isEmpty()) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        if (attachmentIndex < 0 || attachmentIndex >= attachments.size()) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        Map<String, Object> attachment = attachments.get(attachmentIndex);
        String storageKey =
                OnboardingStepPayloads.readNullableString(
                        attachment, OnboardingStepPayloads.STORAGE_KEY);
        if (storageKey == null) {
            throw new ResourceNotFoundException("Onboarding attachment not found");
        }
        String fileName =
                OnboardingStepPayloads.readNullableString(
                        attachment, OnboardingStepPayloads.FILE_NAME);
        String contentType =
                OnboardingStepPayloads.readNullableString(
                        attachment, OnboardingStepPayloads.CONTENT_TYPE);
        InputStream inputStream = objectStorageService.downloadFile(attachmentsBucket, storageKey);
        return new StoredUpload(
                inputStream,
                fileName != null ? fileName : fallbackFileName,
                AllowedAttachmentMediaTypes.forDelivery(contentType));
    }

    public record StoredUpload(InputStream inputStream, String fileName, String contentType) {}

    private OnboardingInvitation requireEditableOwnInvitation(long userId, long invitationId) {
        OnboardingInvitation invitation = requireOwnInvitation(userId, invitationId);
        if (!isEditable(invitation)) {
            throw new ConflictException("Onboarding invitation is not editable");
        }
        if (membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                userId, invitation.getTargetOrganizationId(), Instant.now())) {
            throw new ConflictException(
                    "Onboarding for existing members is recorded by an administrator");
        }
        return invitation;
    }

    private OnboardingInvitation requireEditableInvitationForOrganization(
            long organizationId, long invitationId) {
        OnboardingInvitation invitation =
                requireInvitationForOrganization(organizationId, invitationId);
        if (!isEditable(invitation)) {
            throw new ConflictException("Onboarding invitation is not editable");
        }
        return invitation;
    }

    private StandardOnboardingStep requireConfiguredStep(
            OnboardingInvitation invitation, OnboardingStepType stepType) {
        Organization organization =
                organizationRepository
                        .findById(invitation.getTargetOrganizationId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: "
                                                        + invitation.getTargetOrganizationId()));
        Standard standard =
                effectiveStandardResolver
                        .resolveForOrganization(organization)
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "Organization has no onboarding steps"));
        return standardOnboardingStepService.listSteps(standard).stream()
                .filter(step -> step.getStepType() == stepType)
                .findFirst()
                .orElseThrow(
                        () ->
                                new ConflictException(
                                        "Onboarding step is not configured: " + stepType));
    }

    private void requireDocumentKey(
            OnboardingInvitation invitation, OnboardingStepType stepType, String configKey) {
        requireConfiguredStep(invitation, stepType);
        if (stepType == OnboardingStepType.ENTRY_DOCUMENTS) {
            StandardOnboardingStep step =
                    requireConfiguredStep(invitation, OnboardingStepType.ENTRY_DOCUMENTS);
            OnboardingStepConfig parsed =
                    handlerRegistry
                            .require(OnboardingStepType.ENTRY_DOCUMENTS)
                            .parseConfig(step.getConfig());
            if (!(parsed instanceof EntryDocumentsStepConfig entryConfig)
                    || !entryConfig.documentConfigKeys().contains(configKey)) {
                throw new ResourceNotFoundException("Onboarding document not found: " + configKey);
            }
            return;
        }
        if (stepType == OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS) {
            OnboardingStepFulfillmentContext context =
                    new OnboardingStepFulfillmentContext(
                            invitation.getTenantId(),
                            invitation.getUserId(),
                            invitation.getTargetOrganizationId(),
                            invitation.getId());
            if (!organizationEntryDocumentsStepHandler
                    .resolveConfigKeys(context)
                    .contains(configKey)) {
                throw new ResourceNotFoundException("Onboarding document not found: " + configKey);
            }
            return;
        }
        throw new ResourceNotFoundException("Onboarding document not found: " + configKey);
    }

    private static Map<String, Object> findDocument(
            List<Map<String, Object>> documents, String configKey) {
        for (Map<String, Object> document : documents) {
            if (configKey.equals(
                    OnboardingStepPayloads.readString(
                            document, OnboardingStepPayloads.CONFIG_KEY))) {
                return document;
            }
        }
        return null;
    }

    private static void replaceDocument(
            List<Map<String, Object>> documents, Map<String, Object> next) {
        String configKey =
                OnboardingStepPayloads.readString(next, OnboardingStepPayloads.CONFIG_KEY);
        for (int i = 0; i < documents.size(); i++) {
            if (configKey.equals(
                    OnboardingStepPayloads.readString(
                            documents.get(i), OnboardingStepPayloads.CONFIG_KEY))) {
                documents.set(i, next);
                return;
            }
        }
        documents.add(next);
    }

    private void deleteStoredUpload(Map<String, Object> stored) {
        if (stored == null) {
            return;
        }
        for (Map<String, Object> attachment : OnboardingStepPayloads.readAttachments(stored)) {
            deleteStoredAttachment(attachment);
        }
    }

    private void deleteStoredAttachment(Map<String, Object> attachment) {
        String storageKey =
                OnboardingStepPayloads.readNullableString(
                        attachment, OnboardingStepPayloads.STORAGE_KEY);
        if (storageKey == null) {
            return;
        }
        objectStorageTransactionCoordinator.deleteFileAfterCommit(attachmentsBucket, storageKey);
    }

    private static void copyIfPresent(
            Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }
}
