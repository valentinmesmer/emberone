/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.service;

import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionDocument;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionGrantedType;
import com.emberone.trainingcourse.dto.*;
import com.emberone.trainingcourse.event.TrainingCourseRecordChangedEvent;
import com.emberone.trainingcourse.list.TrainingCourseRecordListDefinition;
import com.emberone.trainingcourse.list.TrainingCourseRecordListFilter;
import com.emberone.trainingcourse.list.TrainingCourseRecordListSpecs;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionDocumentRepository;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.domain.UserDocumentLink;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates and reads training course submissions. Submissions originate manually
 * (TRAINING_COURSE_WRITE), from an approved workflow, or from the system; the origin is captured
 * via {@code appLinkType}/{@code appLinkId}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingCourseRecordService {
    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.trainingCourseRecord";

    private final TrainingCourseUserSubmissionRepository submissionRepository;
    private final TrainingCourseUserSubmissionDocumentRepository submissionDocumentRepository;
    private final TenantTrainingCourseTypeRepository tenantTypeRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final UserDocumentService userDocumentService;
    private final TokenService tokenService;
    private final TrainingCourseCatalogService catalogService;
    private final LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;
    private final WorkflowRepository workflowRepository;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AppNavigationResolver appNavigationResolver;

    @Transactional(readOnly = true)
    public TrainingCourseUserRecordPageDto listRecords(
            Long userId, ListQuery query, TrainingCourseRecordListFilter filter) {
        Pageable pageable = TrainingCourseRecordListDefinition.INSTANCE.pageable(query);
        Specification<TrainingCourseUserSubmission> spec =
                Specification.allOf(
                        TrainingCourseRecordListSpecs.forUser(userId),
                        TrainingCourseRecordListDefinition.INSTANCE.searchSpecification(query));
        Page<TrainingCourseUserSubmission> entityPage =
                submissionRepository.findAll(spec, pageable);
        List<Long> ids =
                entityPage.getContent().stream().map(TrainingCourseUserSubmission::getId).toList();
        List<TrainingCourseUserSubmission> hydrated =
                ids.isEmpty() ? List.of() : submissionRepository.findWithGrantedTypesByIdIn(ids);
        List<TrainingCourseUserSubmission> ordered =
                PageReorder.reorderByIds(ids, hydrated, TrainingCourseUserSubmission::getId);
        Page<TrainingCourseUserSubmission> page = PageReorder.pageOf(ordered, entityPage);
        Map<Long, List<Long>> documentIdsBySubmission =
                loadDocumentIdsBySubmission(page.getContent());
        return TrainingCourseUserRecordPageDto.builder()
                .content(
                        page.getContent().stream()
                                .map(
                                        submission ->
                                                toDto(
                                                        submission,
                                                        documentIdsBySubmission.getOrDefault(
                                                                submission.getId(), List.of())))
                                .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrainingCoursePendingSubmissionDto> listPendingSubmissions(Long userId) {
        List<TrainingCoursePendingSubmissionDto> pending = new ArrayList<>();
        for (Workflow workflow : workflowRepository.findOpenedByUser(userId)) {
            if (workflow.getStatus() != WorkflowStatus.OPEN
                    || !WorkflowType.TRAINING_COURSE_RECORD
                            .getId()
                            .equals(workflow.getWorkflowTypeId())) {
                continue;
            }
            TrainingCourseRecordWorkflowPayload payload =
                    readTrainingCoursePayload(workflow.getPayload());
            if (payload == null
                    || payload.getTenantTrainingCourseTypeIds() == null
                    || payload.getTenantTrainingCourseTypeIds().isEmpty()) {
                continue;
            }
            List<Long> typeIds = normalizeTypeIds(payload.getTenantTrainingCourseTypeIds());
            List<String> typeNames = resolveTypeNames(typeIds);
            pending.add(
                    TrainingCoursePendingSubmissionDto.builder()
                            .workflowId(workflow.getId())
                            .name(resolveDisplayName(payload.getName(), typeNames))
                            .tenantTrainingCourseTypeIds(typeIds)
                            .courseTypeNames(typeNames)
                            .startDate(payload.getStartDate())
                            .endDate(payload.getEndDate())
                            .location(payload.getLocation())
                            .hours(payload.getHours())
                            .build());
        }
        pending.sort(
                Comparator.comparing(
                        TrainingCoursePendingSubmissionDto::getEndDate,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return pending;
    }

    @Transactional
    public TrainingCourseUserRecordDto createManual(
            Long organizationId, Long userId, CreateTrainingCourseUserRecordRequest request) {
        Long tenantId = tokenService.requireTenantId();
        validateDateRange(request.getStartDate(), request.getEndDate());
        List<Long> typeIds = normalizeTypeIds(request.getTenantTrainingCourseTypeIds());
        List<TenantTrainingCourseType> types = resolveTypesForOrg(organizationId, typeIds);
        String name = resolveSubmissionName(request.getName(), types);
        TrainingCourseUserSubmission submission =
                saveSubmission(
                        tenantId,
                        userId,
                        name,
                        request.getStartDate(),
                        request.getEndDate(),
                        normalizeLocation(request.getLocation()),
                        request.getHours(),
                        request.getNotes(),
                        null,
                        null,
                        types);
        List<Long> documentIds =
                linkDocuments(
                        tenantId, userId, submission, request.getUserDocumentIds(), true, name);
        log.info(
                "Created manual training course submission {} for user {} (types {})",
                submission.getId(),
                userId,
                typeIds);
        publishRecordChanged(tenantId, userId);
        return toDto(submission, documentIds);
    }

    @Transactional
    public TrainingCourseUserRecordDto updateManual(
            Long organizationId,
            Long userId,
            Long submissionId,
            UpdateTrainingCourseUserRecordRequest request) {
        Long tenantId = tokenService.requireTenantId();
        validateDateRange(request.getStartDate(), request.getEndDate());
        TrainingCourseUserSubmission submission = requireSubmissionForUser(userId, submissionId);
        for (TrainingCourseUserSubmissionGrantedType grantedType : submission.getGrantedTypes()) {
            catalogService.requireTypeAvailableForOrg(
                    grantedType.getTenantTrainingCourseTypeId(), organizationId);
            TenantTrainingCourseType type =
                    requireType(grantedType.getTenantTrainingCourseTypeId());
            if (type.isArchived()) {
                throw new ConflictException("Training course type is archived");
            }
        }
        if (submission.getGrantedTypes().size() > 1) {
            if (!StringUtils.hasText(request.getName())) {
                throw new ConflictException("name is required when multiple types are granted");
            }
            submission.setName(request.getName().trim());
        }
        submission.setStartDate(request.getStartDate());
        submission.setEndDate(request.getEndDate());
        submission.setLocation(normalizeLocation(request.getLocation()));
        submission.setHours(request.getHours());
        submission.setNotes(request.getNotes());
        submissionRepository.save(submission);
        List<Long> documentIds =
                submissionDocumentRepository.findBySubmission_Id(submission.getId()).stream()
                        .map(TrainingCourseUserSubmissionDocument::getDocumentId)
                        .toList();
        log.info("Updated training course submission {} for user {}", submission.getId(), userId);
        publishRecordChanged(tenantId, userId);
        return toDto(submission, documentIds);
    }

    @Transactional
    public void deleteManual(Long organizationId, Long userId, Long submissionId) {
        Long tenantId = tokenService.requireTenantId();
        TrainingCourseUserSubmission submission = requireSubmissionForUser(userId, submissionId);
        for (TrainingCourseUserSubmissionGrantedType grantedType : submission.getGrantedTypes()) {
            catalogService.requireTypeAvailableForOrg(
                    grantedType.getTenantTrainingCourseTypeId(), organizationId);
        }
        userDocumentService.deleteDocumentsForLinkTargets(
                tenantId, AppLinkType.TRAINING_COURSE_RECORD, List.of(submission.getId()));
        submissionRepository.delete(submission);
        log.info("Deleted training course submission {} for user {}", submission.getId(), userId);
        publishRecordChanged(tenantId, userId);
    }

    @Transactional
    public TrainingCourseUserSubmission createSubmissionFromWorkflow(
            Long tenantId,
            Long userId,
            String name,
            List<Long> tenantTrainingCourseTypeIds,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            BigDecimal hours,
            String notes,
            Long workflowId) {
        validateDateRange(startDate, endDate);
        List<Long> typeIds = normalizeTypeIds(tenantTrainingCourseTypeIds);
        List<TenantTrainingCourseType> types =
                typeIds.stream().map(typeId -> requireType(typeId)).toList();
        for (TenantTrainingCourseType type : types) {
            if (type.isArchived()) {
                throw new ConflictException("Training course type is archived");
            }
        }
        String resolvedName = resolveSubmissionName(name, types);
        TrainingCourseUserSubmission submission =
                saveSubmission(
                        tenantId,
                        userId,
                        resolvedName,
                        startDate,
                        endDate,
                        normalizeLocation(location),
                        hours,
                        notes,
                        AppLinkType.WORKFLOW,
                        workflowId,
                        types);
        log.info(
                "Created workflow training course submission {} for user {} (types {}, workflow {})",
                submission.getId(),
                userId,
                typeIds,
                workflowId);
        publishRecordChanged(tenantId, userId);
        return submission;
    }

    /**
     * Credits a course held by a course offering to the participant's registration. Unlike a
     * workflow submission this does not reject an archived type: the course took place, archiving
     * only hides the type from new use.
     */
    @Transactional
    public TrainingCourseUserSubmission createSubmissionFromCourseRegistration(
            Long tenantId,
            Long userId,
            Long tenantTrainingCourseTypeId,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            String notes,
            BigDecimal hours,
            Long registrationId) {
        validateDateRange(startDate, endDate);
        TenantTrainingCourseType type = requireType(tenantTrainingCourseTypeId);
        TrainingCourseUserSubmission submission =
                saveSubmission(
                        tenantId,
                        userId,
                        resolveSubmissionName(null, List.of(type)),
                        startDate,
                        endDate,
                        normalizeLocation(location),
                        hours,
                        notes,
                        AppLinkType.COURSE_REGISTRATION,
                        registrationId,
                        List.of(type));
        log.info(
                "Created course registration training course submission {} for user {} (type {},"
                        + " registration {})",
                submission.getId(),
                userId,
                tenantTrainingCourseTypeId,
                registrationId);
        publishRecordChanged(tenantId, userId);
        return submission;
    }

    @Transactional
    public TrainingCourseUserSubmission createSubmissionFromOnboarding(
            Long tenantId,
            Long userId,
            Long organizationId,
            String name,
            List<Long> tenantTrainingCourseTypeIds,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            BigDecimal hours,
            String notes,
            Long invitationId,
            Long userDocumentId) {
        validateDateRange(startDate, endDate);
        List<Long> typeIds = normalizeTypeIds(tenantTrainingCourseTypeIds);
        List<TenantTrainingCourseType> types = resolveTypesForOrg(organizationId, typeIds);
        String resolvedName = resolveSubmissionName(name, types);
        TrainingCourseUserSubmission submission =
                saveSubmission(
                        tenantId,
                        userId,
                        resolvedName,
                        startDate,
                        endDate,
                        normalizeLocation(location),
                        hours,
                        notes,
                        AppLinkType.ONBOARDING_INVITATION,
                        invitationId,
                        types);
        if (userDocumentId != null) {
            linkDocuments(
                    tenantId, userId, submission, List.of(userDocumentId), true, resolvedName);
        }
        log.info(
                "Created onboarding training course submission {} for user {} (invitation {})",
                submission.getId(),
                userId,
                invitationId);
        publishRecordChanged(tenantId, userId);
        return submission;
    }

    @Transactional
    public void linkDocumentsFromWorkflow(Long tenantId, Long workflowId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        TrainingCourseUserSubmission submission =
                submissionRepository
                        .findByAppLinkTypeAndAppLinkId(AppLinkType.WORKFLOW, workflowId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Training course submission for workflow not found: "
                                                        + workflowId));
        linkDocuments(
                tenantId,
                submission.getUserId(),
                submission,
                documentIds,
                false,
                submission.getName());
        log.debug(
                "Linked {} document(s) to training course submission {} (workflow {})",
                documentIds.size(),
                submission.getId(),
                workflowId);
    }

    private TrainingCourseUserSubmission saveSubmission(
            Long tenantId,
            Long userId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            BigDecimal hours,
            String notes,
            AppLinkType appLinkType,
            Long appLinkId,
            List<TenantTrainingCourseType> types) {
        TrainingCourseUserSubmission submission =
                TrainingCourseUserSubmission.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .name(name)
                        .startDate(startDate)
                        .endDate(endDate)
                        .location(location)
                        .hours(hours)
                        .notes(notes)
                        .appLinkType(appLinkType)
                        .appLinkId(appLinkId)
                        .build();
        for (TenantTrainingCourseType type : types) {
            submission
                    .getGrantedTypes()
                    .add(
                            TrainingCourseUserSubmissionGrantedType.builder()
                                    .tenantId(tenantId)
                                    .submission(submission)
                                    .tenantTrainingCourseTypeId(type.getId())
                                    .build());
        }
        return submissionRepository.save(submission);
    }

    private String resolveSubmissionName(
            String requestedName, List<TenantTrainingCourseType> types) {
        if (types.size() == 1) {
            String typeName = types.get(0).getName();
            if (StringUtils.hasText(typeName)) {
                return typeName.trim();
            }
        }
        if (!StringUtils.hasText(requestedName)) {
            throw new ConflictException("name is required when multiple types are granted");
        }
        String normalized = requestedName.trim();
        if (normalized.length() > TrainingCourseRecordWorkflowPayload.MAX_NAME_LENGTH) {
            throw new ConflictException("name is too long");
        }
        return normalized;
    }

    private String resolveDisplayName(String requestedName, List<String> typeNames) {
        if (StringUtils.hasText(requestedName)) {
            return requestedName.trim();
        }
        if (typeNames.size() == 1) {
            return typeNames.get(0);
        }
        return String.join(", ", typeNames);
    }

    private List<String> resolveTypeNames(List<Long> typeIds) {
        return typeIds.stream()
                .map(typeId -> tenantTypeRepository.findById(typeId).orElse(null))
                .map(type -> type == null ? null : type.getName())
                .filter(Objects::nonNull)
                .toList();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ConflictException("startDate must be on or before endDate");
        }
    }

    private String normalizeLocation(String location) {
        if (!StringUtils.hasText(location)) {
            return null;
        }
        return location.trim();
    }

    private TrainingCourseUserSubmission requireSubmissionForUser(Long userId, Long submissionId) {
        TrainingCourseUserSubmission submission =
                submissionRepository
                        .findById(submissionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Training course submission not found: "
                                                        + submissionId));
        if (!userId.equals(submission.getUserId())) {
            throw new ResourceNotFoundException(
                    "Training course submission not found: " + submissionId);
        }
        return submission;
    }

    private List<Long> linkDocuments(
            Long tenantId,
            Long userId,
            TrainingCourseUserSubmission submission,
            List<Long> documentIds,
            boolean applyMetadata,
            String metadataName) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
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
            if (!submissionDocumentRepository.existsBySubmission_IdAndDocumentId(
                    submission.getId(), documentId)) {
                submissionDocumentRepository.save(
                        TrainingCourseUserSubmissionDocument.builder()
                                .tenantId(tenantId)
                                .submission(submission)
                                .documentId(documentId)
                                .build());
            }
            if (applyMetadata) {
                String typeName = StringUtils.hasText(metadataName) ? metadataName.trim() : null;
                documentPromotionMetadataSupport.applyToDocument(
                        document,
                        tenantId,
                        DOCUMENT_PROMOTION_PREFIX,
                        typeName,
                        typeName != null ? "Training course proof " + typeName : null);
            }
            addTrainingCourseLinkIfAbsent(document, submission.getId());
            userDocumentRepository.save(document);
            linked.add(documentId);
        }
        return linked;
    }

    private void addTrainingCourseLinkIfAbsent(UserDocument document, Long submissionId) {
        boolean alreadyLinked =
                document.getLinks().stream()
                        .anyMatch(
                                link ->
                                        link.getLinkType() == AppLinkType.TRAINING_COURSE_RECORD
                                                && submissionId.equals(link.getLinkId()));
        if (alreadyLinked) {
            return;
        }
        document.getLinks()
                .add(
                        UserDocumentLink.builder()
                                .tenantId(document.getTenantId())
                                .document(document)
                                .linkType(AppLinkType.TRAINING_COURSE_RECORD)
                                .linkId(submissionId)
                                .build());
    }

    private Map<Long, List<Long>> loadDocumentIdsBySubmission(
            List<TrainingCourseUserSubmission> submissions) {
        if (submissions.isEmpty()) {
            return Map.of();
        }
        Set<Long> submissionIds =
                submissions.stream()
                        .map(TrainingCourseUserSubmission::getId)
                        .collect(Collectors.toSet());
        return submissionDocumentRepository.findBySubmission_IdIn(submissionIds).stream()
                .collect(
                        Collectors.groupingBy(
                                link -> link.getSubmission().getId(),
                                Collectors.mapping(
                                        TrainingCourseUserSubmissionDocument::getDocumentId,
                                        Collectors.toList())));
    }

    private List<Long> normalizeTypeIds(List<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            throw new ConflictException("At least one training course type is required");
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long typeId : typeIds) {
            if (typeId != null) {
                unique.add(typeId);
            }
        }
        if (unique.isEmpty()) {
            throw new ConflictException("At least one training course type is required");
        }
        return List.copyOf(unique);
    }

    private List<TenantTrainingCourseType> resolveTypesForOrg(
            Long organizationId, List<Long> typeIds) {
        List<TenantTrainingCourseType> types = new ArrayList<>();
        for (Long typeId : typeIds) {
            catalogService.requireTypeAvailableForOrg(typeId, organizationId);
            TenantTrainingCourseType type = requireType(typeId);
            if (type.isArchived()) {
                throw new ConflictException("Training course type is archived");
            }
            types.add(type);
        }
        return types;
    }

    private TenantTrainingCourseType requireType(Long tenantTrainingCourseTypeId) {
        return tenantTypeRepository
                .findById(tenantTrainingCourseTypeId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Training course type not found: "
                                                + tenantTrainingCourseTypeId));
    }

    TrainingCourseUserRecordDto toDto(
            TrainingCourseUserSubmission submission, List<Long> documentIds) {
        List<TrainingCourseGrantedTypeDto> grantedTypes =
                submission.getGrantedTypes().stream()
                        .map(
                                grantedType -> {
                                    TenantTrainingCourseType type =
                                            tenantTypeRepository
                                                    .findById(
                                                            grantedType
                                                                    .getTenantTrainingCourseTypeId())
                                                    .orElse(null);
                                    return TrainingCourseGrantedTypeDto.builder()
                                            .tenantTrainingCourseTypeId(
                                                    grantedType.getTenantTrainingCourseTypeId())
                                            .courseTypeName(type == null ? null : type.getName())
                                            .typeArchived(type != null && type.isArchived())
                                            .build();
                                })
                        .sorted(
                                Comparator.comparing(
                                        TrainingCourseGrantedTypeDto::getCourseTypeName,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList();
        return TrainingCourseUserRecordDto.builder()
                .id(submission.getId())
                .userId(submission.getUserId())
                .name(submission.getName())
                .startDate(submission.getStartDate())
                .endDate(submission.getEndDate())
                .location(submission.getLocation())
                .hours(submission.getHours())
                .userDocumentIds(documentIds != null ? List.copyOf(documentIds) : List.of())
                .grantedTypes(grantedTypes)
                .notes(submission.getNotes())
                .appLinkType(
                        submission.getAppLinkType() == null
                                ? null
                                : submission.getAppLinkType().name())
                .appLinkId(submission.getAppLinkId())
                .navigationPath(
                        appNavigationResolver.resolveNavigationPath(
                                submission.getAppLinkType(), submission.getAppLinkId()))
                .build();
    }

    private TrainingCourseRecordWorkflowPayload readTrainingCoursePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(payload, TrainingCourseRecordWorkflowPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void publishRecordChanged(Long tenantId, Long userId) {
        eventPublisher.publishEvent(new TrainingCourseRecordChangedEvent(tenantId, userId));
    }
}
