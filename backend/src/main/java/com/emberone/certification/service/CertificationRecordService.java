/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.domain.CertEvidenceType;
import com.emberone.certification.domain.CertificationSubmissionMethod;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.domain.UserCertificationRecordDocument;
import com.emberone.certification.dto.CreateUserCertificationRecordRequest;
import com.emberone.certification.dto.UserCertificationRecordDto;
import com.emberone.certification.event.CertificationAssignmentChangedEvent;
import com.emberone.certification.event.CertificationEventGrant;
import com.emberone.certification.event.CertificationEventGrantNavigationResolver;
import com.emberone.certification.repository.UserCertificationRecordDocumentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.ListEndpointLimits;
import com.emberone.platform.web.PageDto;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.domain.UserDocumentLink;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates and reads certification records. Records originate manually (CERTIFICATION_WRITE), from
 * an approved workflow, or from the system; the origin is captured via {@code appLinkType}/{@code
 * appLinkId}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationRecordService {
    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.certificationRecord";

    private final UserCertificationRecordRepository recordRepository;
    private final UserCertificationRecordDocumentRepository recordDocumentRepository;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final UserDocumentService userDocumentService;
    private final TokenService tokenService;
    private final CertificationAssignmentService assignmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService;
    private final AppNavigationResolver appNavigationResolver;
    private final CertificationEventGrantNavigationResolver grantNavigationResolver;

    @Transactional(readOnly = true)
    public List<UserCertificationRecordDto> listRecords(Long userId) {
        return mapRecordsToDtos(recordRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public PageDto<UserCertificationRecordDto> pageRecords(Long userId, int page, int size) {
        Pageable pageable = ListEndpointLimits.pageRequest(page, size);
        Page<UserCertificationRecord> result =
                recordRepository.findByUserIdOrderByCompletedDateDescIdDesc(userId, pageable);
        List<UserCertificationRecord> records = result.getContent();
        Map<Long, List<Long>> documentIdsByRecord = loadDocumentIdsByRecord(records);
        List<UserCertificationRecordDto> content =
                records.stream()
                        .map(
                                record ->
                                        toDto(
                                                record,
                                                documentIdsByRecord.getOrDefault(
                                                        record.getId(), List.of())))
                        .toList();
        return PageDto.from(result, content);
    }

    private List<UserCertificationRecordDto> mapRecordsToDtos(
            List<UserCertificationRecord> records) {
        List<UserCertificationRecord> sorted =
                records.stream()
                        .sorted(
                                Comparator.comparing(UserCertificationRecord::getCompletedDate)
                                        .reversed())
                        .toList();
        Map<Long, List<Long>> documentIdsByRecord = loadDocumentIdsByRecord(sorted);
        return sorted.stream()
                .map(
                        record ->
                                toDto(
                                        record,
                                        documentIdsByRecord.getOrDefault(
                                                record.getId(), List.of())))
                .toList();
    }

    /**
     * Resolves expiry for capture paths (manual, workflow, onboarding). Throws when a
     * health-evidence definition would be stored without an expiry date.
     */
    public LocalDate resolveExpiryForCapture(
            TenantCertDefinition definition, LocalDate completedDate, LocalDate explicitExpiry) {
        return resolveExpiry(definition, completedDate, explicitExpiry);
    }

    /**
     * Same rule as {@link #resolveExpiryForCapture} for captures that only carry ids (onboarding).
     */
    @Transactional(readOnly = true)
    public void validateExpiryForCapture(
            Long tenantCertDefinitionId, LocalDate completedDate, LocalDate explicitExpiry) {
        resolveExpiry(requireDefinition(tenantCertDefinitionId), completedDate, explicitExpiry);
    }

    /** Manual record entry by a CERTIFICATION_WRITE holder ({@code appLinkType} stays null). */
    @Transactional
    public UserCertificationRecordDto createManual(
            Long userId, CreateUserCertificationRecordRequest request) {
        Long tenantId = tokenService.requireTenantId();
        TenantCertDefinition definition = requireDefinition(request.getTenantCertDefinitionId());
        if (definition.isArchived()) {
            throw new ConflictException("Certification definition is archived");
        }
        if (!assignmentService.isDefinitionAssignedToUser(userId, definition.getId())) {
            throw new ConflictException("Certification is not assigned to this user");
        }
        Set<CertEvidenceType> evidenceTypes =
                evidenceTypeService.getEvidenceTypes(definition.getId());
        if (!TenantCertDefinitionEvidenceTypeService.allowsManualSubmission(evidenceTypes)) {
            throw new ConflictException(
                    "Manual certification record entry is not available for this definition");
        }
        UserCertificationRecord record =
                recordRepository.save(
                        UserCertificationRecord.builder()
                                .tenantId(tenantId)
                                .userId(userId)
                                .tenantCertDefinitionId(definition.getId())
                                .completedDate(request.getCompletedDate())
                                .expiryDate(
                                        resolveExpiry(
                                                definition,
                                                request.getCompletedDate(),
                                                request.getExpiryDate()))
                                .notes(request.getNotes())
                                .submissionMethod(CertificationSubmissionMethod.MANUAL)
                                .build());
        List<Long> documentIds =
                linkDocuments(
                        tenantId, userId, record, definition, request.getUserDocumentIds(), true);
        log.info(
                "Created manual certification record {} for user {} (definition {})",
                record.getId(),
                userId,
                definition.getId());
        eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        return toDto(record, documentIds);
    }

    /**
     * Create one record per definition from an approved workflow. {@code appLinkType=WORKFLOW},
     * {@code appLinkId=workflowId}.
     */
    @Transactional
    public List<UserCertificationRecord> createFromWorkflow(
            Long tenantId,
            Long userId,
            List<Long> tenantCertDefinitionIds,
            LocalDate completedDate,
            String notes,
            Long workflowId) {
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        if (tenantCertDefinitionIds != null) {
            for (Long definitionId : tenantCertDefinitionIds) {
                if (definitionId != null) {
                    uniqueIds.add(definitionId);
                }
            }
        }
        List<UserCertificationRecord> created = new ArrayList<>();
        for (Long tenantCertDefinitionId : uniqueIds) {
            TenantCertDefinition definition = requireDefinition(tenantCertDefinitionId);
            if (definition.isArchived()) {
                throw new ConflictException("Certification definition is archived");
            }
            UserCertificationRecord record =
                    recordRepository.save(
                            UserCertificationRecord.builder()
                                    .tenantId(tenantId)
                                    .userId(userId)
                                    .tenantCertDefinitionId(definition.getId())
                                    .completedDate(completedDate)
                                    .expiryDate(resolveExpiry(definition, completedDate, null))
                                    .notes(notes)
                                    .appLinkType(AppLinkType.WORKFLOW)
                                    .appLinkId(workflowId)
                                    .submissionMethod(CertificationSubmissionMethod.WORKFLOW)
                                    .build());
            created.add(record);
            log.info(
                    "Created workflow certification record {} for user {} (definition {}, workflow {})",
                    record.getId(),
                    userId,
                    definition.getId(),
                    workflowId);
        }
        if (!created.isEmpty()) {
            eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        }
        return created;
    }

    @Transactional
    public UserCertificationRecord createFromOnboarding(
            Long tenantId,
            Long userId,
            Long tenantCertDefinitionId,
            LocalDate completedDate,
            LocalDate expiryDate,
            String notes,
            Long invitationId,
            Long userDocumentId) {
        TenantCertDefinition definition = requireDefinition(tenantCertDefinitionId);
        if (definition.isArchived()) {
            throw new ConflictException("Certification definition is archived");
        }
        UserCertificationRecord record =
                recordRepository.save(
                        UserCertificationRecord.builder()
                                .tenantId(tenantId)
                                .userId(userId)
                                .tenantCertDefinitionId(definition.getId())
                                .completedDate(completedDate)
                                .expiryDate(resolveExpiry(definition, completedDate, expiryDate))
                                .notes(notes)
                                .appLinkType(AppLinkType.ONBOARDING_INVITATION)
                                .appLinkId(invitationId)
                                .submissionMethod(CertificationSubmissionMethod.ONBOARDING)
                                .build());
        if (userDocumentId != null) {
            linkDocuments(tenantId, userId, record, definition, List.of(userDocumentId), true);
        }
        log.info(
                "Created onboarding certification record {} for user {} (definition {}, invitation {})",
                record.getId(),
                userId,
                definition.getId(),
                invitationId);
        eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        return record;
    }

    /**
     * Create a record from an event-based certification grant. {@code
     * appLinkType=CERTIFICATION_EVENT_GRANT}, {@code appLinkId=grantId}.
     */
    @Transactional
    public UserCertificationRecord createFromEventGrant(
            Long tenantId,
            Long userId,
            Long tenantCertDefinitionId,
            CertificationEventGrant grant) {
        TenantCertDefinition definition = requireDefinition(tenantCertDefinitionId);
        if (definition.isArchived()) {
            throw new ConflictException("Certification definition is archived");
        }
        UserCertificationRecord record =
                recordRepository.save(
                        UserCertificationRecord.builder()
                                .tenantId(tenantId)
                                .userId(userId)
                                .tenantCertDefinitionId(definition.getId())
                                .completedDate(grant.getCompletedDate())
                                .expiryDate(
                                        resolveExpiry(definition, grant.getCompletedDate(), null))
                                .notes(grant.getNotes())
                                .appLinkType(AppLinkType.CERTIFICATION_EVENT_GRANT)
                                .appLinkId(grant.getId())
                                .submissionMethod(CertificationSubmissionMethod.EVENT)
                                .build());
        log.info(
                "Created event-grant certification record {} for user {} (definition {}, grant {})",
                record.getId(),
                userId,
                definition.getId(),
                grant.getId());
        eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        return record;
    }

    /** Syncs completed date, expiry and notes from the event grant onto an existing record. */
    @Transactional
    public UserCertificationRecord syncFromEventGrant(
            UserCertificationRecord record, CertificationEventGrant grant) {
        TenantCertDefinition definition = requireDefinition(record.getTenantCertDefinitionId());
        record.setCompletedDate(grant.getCompletedDate());
        record.setExpiryDate(resolveExpiry(definition, grant.getCompletedDate(), null));
        record.setNotes(grant.getNotes());
        return recordRepository.save(record);
    }

    /**
     * Deletes a record created from an event grant and publishes {@link
     * CertificationAssignmentChangedEvent} so reminders/todos stay consistent (mirrors {@link
     * #createFromEventGrant}).
     */
    @Transactional
    public void deleteFromEventGrant(UserCertificationRecord record) {
        Long tenantId = record.getTenantId();
        Long userId = record.getUserId();
        List<Long> documentIds =
                recordDocumentRepository.findByRecord_Id(record.getId()).stream()
                        .map(UserCertificationRecordDocument::getDocumentId)
                        .filter(
                                documentId ->
                                        !recordDocumentRepository.existsByDocumentIdAndRecord_IdNot(
                                                documentId, record.getId()))
                        .toList();
        userDocumentService.deleteDocumentsByIds(tenantId, documentIds);
        recordRepository.delete(record);
        log.info("Removed event-grant certification record {} for user {}", record.getId(), userId);
        eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
    }

    /**
     * Links promoted workflow documents to every certification record created from that workflow.
     */
    @Transactional
    public void linkDocumentsFromWorkflow(Long tenantId, Long workflowId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        List<UserCertificationRecord> records =
                recordRepository.findAllByAppLinkTypeAndAppLinkId(AppLinkType.WORKFLOW, workflowId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Certification record for workflow not found: " + workflowId);
        }
        for (UserCertificationRecord record : records) {
            TenantCertDefinition definition = requireDefinition(record.getTenantCertDefinitionId());
            linkDocuments(tenantId, record.getUserId(), record, definition, documentIds, false);
            log.debug(
                    "Linked {} document(s) to certification record {} (workflow {})",
                    documentIds.size(),
                    record.getId(),
                    workflowId);
        }
    }

    private List<Long> linkDocuments(
            Long tenantId,
            Long userId,
            UserCertificationRecord record,
            TenantCertDefinition definition,
            List<Long> documentIds,
            boolean applyCertificationMetadata) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        Long tenantCertDefinitionId = definition.getId();
        Set<Long> uniqueIds = new LinkedHashSet<>(documentIds);
        List<Long> linked = new ArrayList<>();
        for (Long documentId : uniqueIds) {
            if (documentId == null) {
                continue;
            }
            UserDocument document =
                    userDocumentRepository
                            .findByIdAndUserId(documentId, userId)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Document not found: " + documentId));
            if (!recordDocumentRepository.existsByRecord_IdAndDocumentId(
                    record.getId(), documentId)) {
                recordDocumentRepository.save(
                        UserCertificationRecordDocument.builder()
                                .tenantId(tenantId)
                                .record(record)
                                .documentId(documentId)
                                .build());
            }
            if (applyCertificationMetadata) {
                String definitionName =
                        StringUtils.hasText(definition.getName())
                                ? definition.getName().trim()
                                : null;
                documentPromotionMetadataSupport.applyToDocument(
                        document,
                        tenantId,
                        DOCUMENT_PROMOTION_PREFIX,
                        definitionName,
                        definitionName != null ? "Certification proof " + definitionName : null);
            }
            addCertificationLinkIfAbsent(document, tenantCertDefinitionId);
            userDocumentRepository.save(document);
            linked.add(documentId);
        }
        return linked;
    }

    private void addCertificationLinkIfAbsent(UserDocument document, Long tenantCertDefinitionId) {
        boolean alreadyLinked =
                document.getLinks().stream()
                        .anyMatch(
                                link ->
                                        link.getLinkType() == AppLinkType.CERTIFICATION_RECORD
                                                && tenantCertDefinitionId.equals(link.getLinkId()));
        if (alreadyLinked) {
            return;
        }
        document.getLinks()
                .add(
                        UserDocumentLink.builder()
                                .tenantId(document.getTenantId())
                                .document(document)
                                .linkType(AppLinkType.CERTIFICATION_RECORD)
                                .linkId(tenantCertDefinitionId)
                                .build());
    }

    private Map<Long, List<Long>> loadDocumentIdsByRecord(List<UserCertificationRecord> records) {
        if (records.isEmpty()) {
            return Map.of();
        }
        Set<Long> recordIds =
                records.stream().map(UserCertificationRecord::getId).collect(Collectors.toSet());
        return recordDocumentRepository.findByRecord_IdIn(recordIds).stream()
                .collect(
                        Collectors.groupingBy(
                                link -> link.getRecord().getId(),
                                Collectors.mapping(
                                        UserCertificationRecordDocument::getDocumentId,
                                        Collectors.toList())));
    }

    private LocalDate resolveExpiry(
            TenantCertDefinition definition, LocalDate completedDate, LocalDate explicit) {
        LocalDate resolved;
        if (explicit != null) {
            resolved = explicit;
        } else if (definition.getIntervalDays() == null) {
            resolved = null;
        } else {
            resolved = completedDate.plusDays(definition.getIntervalDays());
        }
        return resolved;
    }

    private TenantCertDefinition requireDefinition(Long tenantCertDefinitionId) {
        return tenantDefinitionRepository
                .findById(tenantCertDefinitionId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Certification definition not found: "
                                                + tenantCertDefinitionId));
    }

    UserCertificationRecordDto toDto(UserCertificationRecord record, List<Long> documentIds) {
        TenantCertDefinition definition =
                tenantDefinitionRepository
                        .findById(record.getTenantCertDefinitionId())
                        .orElse(null);
        CertificationEventGrantNavigationResolver.Resolved grant =
                record.getAppLinkType() == AppLinkType.CERTIFICATION_EVENT_GRANT
                        ? grantNavigationResolver.resolve(record.getAppLinkId())
                        : null;
        return UserCertificationRecordDto.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .tenantCertDefinitionId(record.getTenantCertDefinitionId())
                .certDefinitionName(definition == null ? null : definition.getName())
                .definitionArchived(definition != null && definition.isArchived())
                .completedDate(record.getCompletedDate())
                .expiryDate(record.getExpiryDate())
                .userDocumentIds(documentIds != null ? List.copyOf(documentIds) : List.of())
                .notes(record.getNotes())
                .appLinkType(
                        record.getAppLinkType() == null ? null : record.getAppLinkType().name())
                .appLinkId(record.getAppLinkId())
                .submissionMethod(
                        record.getSubmissionMethod() == null
                                ? null
                                : record.getSubmissionMethod().name())
                .eventType(
                        grant == null || grant.eventType() == null
                                ? null
                                : grant.eventType().name())
                .eventOrganizationId(grant == null ? null : grant.organizationId())
                .navigationPath(
                        grant != null
                                ? grant.navigationPath()
                                : appNavigationResolver.resolveNavigationPath(
                                        record.getAppLinkType(), record.getAppLinkId()))
                .build();
    }
}
