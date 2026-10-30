/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.service;

import com.emberone.drivinglicense.domain.UserDrivingLicense;
import com.emberone.drivinglicense.domain.UserDrivingLicenseClass;
import com.emberone.drivinglicense.domain.UserDrivingLicenseDocument;
import com.emberone.drivinglicense.dto.*;
import com.emberone.drivinglicense.event.DrivingLicenseRecordChangedEvent;
import com.emberone.drivinglicense.repository.UserDrivingLicenseDocumentRepository;
import com.emberone.drivinglicense.repository.UserDrivingLicenseRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.domain.UserDocumentLink;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.drivinglicense.DrivingLicenseRecordWorkflowPayload;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingLicenseRecordService {
    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.drivingLicenseRecord";

    private final UserDrivingLicenseRepository licenseRepository;
    private final UserDrivingLicenseDocumentRepository licenseDocumentRepository;
    private final TenantDrivingLicenseDefinitionRepository tenantDefinitionRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final UserDocumentService userDocumentService;
    private final TokenService tokenService;
    private final DrivingLicenseCatalogService catalogService;
    private final LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;
    private final WorkflowRepository workflowRepository;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AppNavigationResolver appNavigationResolver;

    @Transactional(readOnly = true)
    public Optional<DrivingLicenseDto> findLicense(Long userId) {
        return licenseRepository.findByUserId(userId).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<DrivingLicensePendingWorkflowDto> findPendingWorkflow(Long userId) {
        for (Workflow workflow : workflowRepository.findOpenedByUser(userId)) {
            if (workflow.getStatus() != WorkflowStatus.OPEN
                    || !WorkflowType.DRIVING_LICENSE_RECORD
                            .getId()
                            .equals(workflow.getWorkflowTypeId())) {
                continue;
            }
            DrivingLicenseRecordWorkflowPayload payload =
                    readDrivingLicensePayload(workflow.getPayload());
            if (payload == null || payload.getClasses() == null || payload.getClasses().isEmpty()) {
                continue;
            }
            return Optional.of(
                    DrivingLicensePendingWorkflowDto.builder()
                            .workflowId(workflow.getId())
                            .issueDate(payload.getIssueDate())
                            .documentExpirationDate(payload.getDocumentExpirationDate())
                            .licenseNumber(payload.getLicenseNumber())
                            .issuingAuthority(payload.getIssuingAuthority())
                            .classes(
                                    payload.getClasses().stream()
                                            .map(
                                                    classPayload -> {
                                                        DrivingLicenseClassInput input =
                                                                new DrivingLicenseClassInput();
                                                        input.setTenantDrivingLicenseDefinitionId(
                                                                classPayload
                                                                        .getTenantDrivingLicenseDefinitionId());
                                                        input.setGrantDate(
                                                                classPayload.getGrantDate());
                                                        input.setExpirationDate(
                                                                classPayload.getExpirationDate());
                                                        return input;
                                                    })
                                            .toList())
                            .build());
        }
        return Optional.empty();
    }

    @Transactional
    public DrivingLicenseDto createManual(
            Long organizationId, Long userId, CreateDrivingLicenseRequest request) {
        Long tenantId = tokenService.requireTenantId();
        if (licenseRepository.findByUserId(userId).isPresent()) {
            throw new ConflictException("Driving license already exists for user");
        }
        List<TenantDrivingLicenseDefinition> definitions =
                resolveDefinitionsForOrg(organizationId, request.getClasses());
        UserDrivingLicense license =
                saveLicense(
                        tenantId,
                        userId,
                        request.getIssueDate(),
                        request.getDocumentExpirationDate(),
                        normalizeOptional(request.getLicenseNumber()),
                        normalizeOptional(request.getIssuingAuthority()),
                        request.getNotes(),
                        null,
                        null,
                        definitions,
                        request.getClasses());
        List<Long> documentIds =
                linkDocuments(tenantId, userId, license, request.getUserDocumentIds(), true, false);
        log.info("Created manual driving license {} for user {}", license.getId(), userId);
        publishRecordChanged(tenantId, userId);
        return toDto(license, documentIds);
    }

    @Transactional
    public DrivingLicenseDto updateManual(
            Long organizationId, Long userId, UpdateDrivingLicenseRequest request) {
        Long tenantId = tokenService.requireTenantId();
        UserDrivingLicense license = requireLicenseForUser(userId);
        List<DrivingLicenseClassInput> classInputs =
                request.getClasses() != null ? request.getClasses() : List.of();
        List<TenantDrivingLicenseDefinition> definitions =
                classInputs.isEmpty()
                        ? List.of()
                        : resolveDefinitionsForOrg(organizationId, classInputs);
        applyLicenseFields(
                license,
                request.getIssueDate(),
                request.getDocumentExpirationDate(),
                normalizeOptional(request.getLicenseNumber()),
                normalizeOptional(request.getIssuingAuthority()),
                request.getNotes());
        syncActiveClasses(tenantId, license, definitions, classInputs);
        licenseRepository.save(license);
        List<Long> documentIds;
        if (request.getUserDocumentIds() != null && !request.getUserDocumentIds().isEmpty()) {
            documentIds =
                    linkDocuments(
                            tenantId, userId, license, request.getUserDocumentIds(), true, true);
        } else {
            documentIds =
                    licenseDocumentRepository.findByLicense_Id(license.getId()).stream()
                            .map(UserDrivingLicenseDocument::getDocumentId)
                            .toList();
        }
        log.info("Updated driving license {} for user {}", license.getId(), userId);
        publishRecordChanged(tenantId, userId);
        return toDto(license, documentIds);
    }

    @Transactional
    public void deleteManual(Long organizationId, Long userId) {
        Long tenantId = tokenService.requireTenantId();
        UserDrivingLicense license = requireLicenseForUser(userId);
        for (UserDrivingLicenseClass licenseClass : license.getClasses()) {
            catalogService.requireDefinitionAvailableForOrg(
                    licenseClass.getTenantDrivingLicenseDefinitionId(), organizationId);
        }
        userDocumentService.deleteDocumentsForLinkTargets(
                tenantId, AppLinkType.DRIVING_LICENSE_RECORD, List.of(license.getId()));
        licenseRepository.delete(license);
        log.info("Deleted driving license {} for user {}", license.getId(), userId);
        publishRecordChanged(tenantId, userId);
    }

    @Transactional
    public UserDrivingLicense upsertFromWorkflow(
            Long tenantId,
            Long userId,
            DrivingLicenseRecordWorkflowPayload payload,
            Long workflowId) {
        List<DrivingLicenseClassInput> classInputs =
                payload.getClasses().stream()
                        .map(
                                classPayload -> {
                                    DrivingLicenseClassInput input = new DrivingLicenseClassInput();
                                    input.setTenantDrivingLicenseDefinitionId(
                                            classPayload.getTenantDrivingLicenseDefinitionId());
                                    input.setGrantDate(classPayload.getGrantDate());
                                    input.setExpirationDate(classPayload.getExpirationDate());
                                    return input;
                                })
                        .toList();
        List<TenantDrivingLicenseDefinition> definitions = resolveDefinitions(classInputs);
        Optional<UserDrivingLicense> existing = licenseRepository.findByUserId(userId);
        UserDrivingLicense license;
        if (existing.isPresent()) {
            license = existing.get();
            applyLicenseFields(
                    license,
                    payload.getIssueDate(),
                    payload.getDocumentExpirationDate(),
                    normalizeOptional(payload.getLicenseNumber()),
                    normalizeOptional(payload.getIssuingAuthority()),
                    payload.getNotes());
            license.setAppLinkType(AppLinkType.WORKFLOW);
            license.setAppLinkId(workflowId);
            syncActiveClasses(tenantId, license, definitions, classInputs);
            licenseRepository.save(license);
            log.info(
                    "Updated workflow driving license {} for user {} (workflow {})",
                    license.getId(),
                    userId,
                    workflowId);
        } else {
            license =
                    saveLicense(
                            tenantId,
                            userId,
                            payload.getIssueDate(),
                            payload.getDocumentExpirationDate(),
                            normalizeOptional(payload.getLicenseNumber()),
                            normalizeOptional(payload.getIssuingAuthority()),
                            payload.getNotes(),
                            AppLinkType.WORKFLOW,
                            workflowId,
                            definitions,
                            classInputs);
            log.info(
                    "Created workflow driving license {} for user {} (workflow {})",
                    license.getId(),
                    userId,
                    workflowId);
        }
        publishRecordChanged(tenantId, userId);
        return license;
    }

    @Transactional
    public void linkDocumentsFromWorkflow(Long tenantId, Long workflowId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        UserDrivingLicense license =
                licenseRepository
                        .findByAppLinkTypeAndAppLinkId(AppLinkType.WORKFLOW, workflowId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Driving license for workflow not found: "
                                                        + workflowId));
        linkDocuments(tenantId, license.getUserId(), license, documentIds, false, true);
        log.debug(
                "Linked {} document(s) to driving license {} (workflow {})",
                documentIds.size(),
                license.getId(),
                workflowId);
    }

    @Transactional
    public UserDrivingLicense upsertFromOnboarding(
            Long tenantId,
            Long userId,
            Long organizationId,
            LocalDate issueDate,
            LocalDate documentExpirationDate,
            String licenseNumber,
            String issuingAuthority,
            String notes,
            List<DrivingLicenseClassInput> classInputs,
            Long invitationId,
            Long userDocumentId) {
        List<TenantDrivingLicenseDefinition> definitions =
                resolveDefinitionsForOrg(organizationId, classInputs);
        Optional<UserDrivingLicense> existing = licenseRepository.findByUserId(userId);
        UserDrivingLicense license;
        if (existing.isPresent()) {
            license = existing.get();
            applyLicenseFields(
                    license,
                    issueDate,
                    documentExpirationDate,
                    normalizeOptional(licenseNumber),
                    normalizeOptional(issuingAuthority),
                    notes);
            license.setAppLinkType(AppLinkType.ONBOARDING_INVITATION);
            license.setAppLinkId(invitationId);
            syncActiveClasses(tenantId, license, definitions, classInputs);
            licenseRepository.save(license);
            log.info(
                    "Updated onboarding driving license {} for user {} (invitation {})",
                    license.getId(),
                    userId,
                    invitationId);
        } else {
            license =
                    saveLicense(
                            tenantId,
                            userId,
                            issueDate,
                            documentExpirationDate,
                            normalizeOptional(licenseNumber),
                            normalizeOptional(issuingAuthority),
                            notes,
                            AppLinkType.ONBOARDING_INVITATION,
                            invitationId,
                            definitions,
                            classInputs);
            log.info(
                    "Created onboarding driving license {} for user {} (invitation {})",
                    license.getId(),
                    userId,
                    invitationId);
        }
        if (userDocumentId != null) {
            linkDocuments(tenantId, userId, license, List.of(userDocumentId), true, false);
        }
        publishRecordChanged(tenantId, userId);
        return license;
    }

    private UserDrivingLicense saveLicense(
            Long tenantId,
            Long userId,
            LocalDate issueDate,
            LocalDate documentExpirationDate,
            String licenseNumber,
            String issuingAuthority,
            String notes,
            AppLinkType appLinkType,
            Long appLinkId,
            List<TenantDrivingLicenseDefinition> definitions,
            List<DrivingLicenseClassInput> classInputs) {
        UserDrivingLicense license =
                UserDrivingLicense.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .issueDate(issueDate)
                        .documentExpirationDate(documentExpirationDate)
                        .licenseNumber(licenseNumber)
                        .issuingAuthority(issuingAuthority)
                        .notes(notes)
                        .appLinkType(appLinkType)
                        .appLinkId(appLinkId)
                        .build();
        addClasses(tenantId, license, definitions, classInputs);
        return licenseRepository.save(license);
    }

    private void applyLicenseFields(
            UserDrivingLicense license,
            LocalDate issueDate,
            LocalDate documentExpirationDate,
            String licenseNumber,
            String issuingAuthority,
            String notes) {
        license.setIssueDate(issueDate);
        license.setDocumentExpirationDate(documentExpirationDate);
        license.setLicenseNumber(licenseNumber);
        license.setIssuingAuthority(issuingAuthority);
        license.setNotes(notes);
    }

    private void addClasses(
            Long tenantId,
            UserDrivingLicense license,
            List<TenantDrivingLicenseDefinition> definitions,
            List<DrivingLicenseClassInput> classInputs) {
        Map<Long, DrivingLicenseClassInput> inputByDefinitionId =
                classInputs.stream()
                        .collect(
                                Collectors.toMap(
                                        DrivingLicenseClassInput
                                                ::getTenantDrivingLicenseDefinitionId,
                                        input -> input,
                                        (left, right) -> right,
                                        LinkedHashMap::new));
        for (TenantDrivingLicenseDefinition definition : definitions) {
            DrivingLicenseClassInput input = inputByDefinitionId.get(definition.getId());
            license.getClasses()
                    .add(
                            UserDrivingLicenseClass.builder()
                                    .tenantId(tenantId)
                                    .license(license)
                                    .tenantDrivingLicenseDefinitionId(definition.getId())
                                    .grantDate(input == null ? null : input.getGrantDate())
                                    .expirationDate(
                                            input == null ? null : input.getExpirationDate())
                                    .build());
        }
    }

    private void syncActiveClasses(
            Long tenantId,
            UserDrivingLicense license,
            List<TenantDrivingLicenseDefinition> definitions,
            List<DrivingLicenseClassInput> classInputs) {
        Set<Long> activeDefinitionIds =
                classInputs.stream()
                        .map(DrivingLicenseClassInput::getTenantDrivingLicenseDefinitionId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        license.getClasses()
                .removeIf(
                        licenseClass ->
                                !activeDefinitionIds.contains(
                                        licenseClass.getTenantDrivingLicenseDefinitionId()));
        Map<Long, UserDrivingLicenseClass> existingByDefinitionId =
                license.getClasses().stream()
                        .collect(
                                Collectors.toMap(
                                        UserDrivingLicenseClass
                                                ::getTenantDrivingLicenseDefinitionId,
                                        licenseClass -> licenseClass,
                                        (left, right) -> left,
                                        LinkedHashMap::new));
        Map<Long, DrivingLicenseClassInput> inputByDefinitionId =
                classInputs.stream()
                        .collect(
                                Collectors.toMap(
                                        DrivingLicenseClassInput
                                                ::getTenantDrivingLicenseDefinitionId,
                                        input -> input,
                                        (left, right) -> right,
                                        LinkedHashMap::new));
        for (TenantDrivingLicenseDefinition definition : definitions) {
            DrivingLicenseClassInput input = inputByDefinitionId.get(definition.getId());
            UserDrivingLicenseClass existing = existingByDefinitionId.get(definition.getId());
            if (existing != null) {
                existing.setGrantDate(input == null ? null : input.getGrantDate());
                existing.setExpirationDate(input == null ? null : input.getExpirationDate());
            } else {
                license.getClasses()
                        .add(
                                UserDrivingLicenseClass.builder()
                                        .tenantId(tenantId)
                                        .license(license)
                                        .tenantDrivingLicenseDefinitionId(definition.getId())
                                        .grantDate(input == null ? null : input.getGrantDate())
                                        .expirationDate(
                                                input == null ? null : input.getExpirationDate())
                                        .build());
            }
        }
    }

    private List<TenantDrivingLicenseDefinition> resolveDefinitionsForOrg(
            Long organizationId, List<DrivingLicenseClassInput> classInputs) {
        List<TenantDrivingLicenseDefinition> definitions = resolveDefinitions(classInputs);
        for (TenantDrivingLicenseDefinition definition : definitions) {
            catalogService.requireDefinitionAvailableForOrg(definition.getId(), organizationId);
        }
        return definitions;
    }

    private List<TenantDrivingLicenseDefinition> resolveDefinitions(
            List<DrivingLicenseClassInput> classInputs) {
        if (classInputs == null || classInputs.isEmpty()) {
            throw new ConflictException("At least one driving license class is required");
        }
        Set<Long> uniqueDefinitionIds = new LinkedHashSet<>();
        for (DrivingLicenseClassInput classInput : classInputs) {
            Long definitionId =
                    requireDefinitionId(classInput.getTenantDrivingLicenseDefinitionId());
            if (!uniqueDefinitionIds.add(definitionId)) {
                throw new ConflictException("Duplicate driving license class in request");
            }
        }
        List<TenantDrivingLicenseDefinition> definitions = new ArrayList<>();
        for (Long definitionId : uniqueDefinitionIds) {
            TenantDrivingLicenseDefinition definition = requireDefinition(definitionId);
            if (definition.isArchived()) {
                throw new ConflictException("Driving license definition is archived");
            }
            definitions.add(definition);
        }
        return definitions;
    }

    private UserDrivingLicense requireLicenseForUser(Long userId) {
        return licenseRepository
                .findByUserId(userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Driving license not found for user: " + userId));
    }

    private List<Long> linkDocuments(
            Long tenantId,
            Long userId,
            UserDrivingLicense license,
            List<Long> documentIds,
            boolean applyMetadata,
            boolean replaceExisting) {
        if (replaceExisting) {
            removeReplacedDocuments(userId, license, documentIds);
        }
        if (documentIds == null || documentIds.isEmpty()) {
            return licenseDocumentRepository.findByLicense_Id(license.getId()).stream()
                    .map(UserDrivingLicenseDocument::getDocumentId)
                    .toList();
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
            if (!licenseDocumentRepository.existsByLicense_IdAndDocumentId(
                    license.getId(), documentId)) {
                licenseDocumentRepository.save(
                        UserDrivingLicenseDocument.builder()
                                .tenantId(tenantId)
                                .license(license)
                                .documentId(documentId)
                                .build());
            }
            if (applyMetadata) {
                documentPromotionMetadataSupport.applyToDocument(
                        document,
                        tenantId,
                        DOCUMENT_PROMOTION_PREFIX,
                        null,
                        "Driving license proof");
            }
            addDrivingLicenseLinkIfAbsent(document, license.getId());
            userDocumentRepository.save(document);
            linked.add(documentId);
        }
        return linked;
    }

    private void removeReplacedDocuments(
            Long userId, UserDrivingLicense license, List<Long> keepDocumentIds) {
        Set<Long> keepIds =
                keepDocumentIds == null
                        ? Set.of()
                        : keepDocumentIds.stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (UserDrivingLicenseDocument link :
                licenseDocumentRepository.findByLicense_Id(license.getId())) {
            if (keepIds.contains(link.getDocumentId())) {
                continue;
            }
            licenseDocumentRepository.delete(link);
        }
    }

    private void addDrivingLicenseLinkIfAbsent(UserDocument document, Long licenseId) {
        boolean alreadyLinked =
                document.getLinks().stream()
                        .anyMatch(
                                link ->
                                        link.getLinkType() == AppLinkType.DRIVING_LICENSE_RECORD
                                                && licenseId.equals(link.getLinkId()));
        if (alreadyLinked) {
            return;
        }
        document.getLinks()
                .add(
                        UserDocumentLink.builder()
                                .tenantId(document.getTenantId())
                                .document(document)
                                .linkType(AppLinkType.DRIVING_LICENSE_RECORD)
                                .linkId(licenseId)
                                .build());
    }

    private Long requireDefinitionId(Long definitionId) {
        if (definitionId == null) {
            throw new ConflictException("tenantDrivingLicenseDefinitionId is required");
        }
        return definitionId;
    }

    private TenantDrivingLicenseDefinition requireDefinition(
            Long tenantDrivingLicenseDefinitionId) {
        return tenantDefinitionRepository
                .findById(tenantDrivingLicenseDefinitionId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Driving license definition not found: "
                                                + tenantDrivingLicenseDefinitionId));
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    DrivingLicenseDto toDto(UserDrivingLicense license) {
        List<Long> documentIds =
                licenseDocumentRepository.findByLicense_Id(license.getId()).stream()
                        .map(UserDrivingLicenseDocument::getDocumentId)
                        .toList();
        return toDto(license, documentIds);
    }

    private DrivingLicenseDto toDto(UserDrivingLicense license, List<Long> documentIds) {
        List<DrivingLicenseClassDto> classes =
                license.getClasses().stream()
                        .map(
                                licenseClass -> {
                                    TenantDrivingLicenseDefinition definition =
                                            tenantDefinitionRepository
                                                    .findById(
                                                            licenseClass
                                                                    .getTenantDrivingLicenseDefinitionId())
                                                    .orElse(null);
                                    return DrivingLicenseClassDto.builder()
                                            .tenantDrivingLicenseDefinitionId(
                                                    licenseClass
                                                            .getTenantDrivingLicenseDefinitionId())
                                            .className(
                                                    definition == null
                                                            ? null
                                                            : definition.getName())
                                            .abbreviation(
                                                    definition == null
                                                            ? null
                                                            : definition.getConfigKey())
                                            .definitionArchived(
                                                    definition != null && definition.isArchived())
                                            .grantDate(licenseClass.getGrantDate())
                                            .expirationDate(licenseClass.getExpirationDate())
                                            .build();
                                })
                        .sorted(
                                Comparator.comparing(
                                        DrivingLicenseClassDto::getAbbreviation,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList();
        return DrivingLicenseDto.builder()
                .id(license.getId())
                .userId(license.getUserId())
                .issueDate(license.getIssueDate())
                .documentExpirationDate(license.getDocumentExpirationDate())
                .licenseNumber(license.getLicenseNumber())
                .issuingAuthority(license.getIssuingAuthority())
                .userDocumentIds(documentIds != null ? List.copyOf(documentIds) : List.of())
                .classes(classes)
                .notes(license.getNotes())
                .appLinkType(
                        license.getAppLinkType() == null ? null : license.getAppLinkType().name())
                .appLinkId(license.getAppLinkId())
                .navigationPath(
                        appNavigationResolver.resolveNavigationPath(
                                license.getAppLinkType(), license.getAppLinkId()))
                .build();
    }

    private DrivingLicenseRecordWorkflowPayload readDrivingLicensePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(payload, DrivingLicenseRecordWorkflowPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void publishRecordChanged(Long tenantId, Long userId) {
        eventPublisher.publishEvent(new DrivingLicenseRecordChangedEvent(tenantId, userId));
    }
}
