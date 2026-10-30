/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.domain.CertificationAssignmentSource;
import com.emberone.certification.domain.UserCertificationAssignment;
import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.certification.dto.CreateUserCertificationAssignmentRequest;
import com.emberone.certification.dto.UserCertificationAssignmentDto;
import com.emberone.certification.event.CertificationAssignmentChangedEvent;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.AccessDeniedException;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.ListEndpointLimits;
import com.emberone.platform.web.PageDto;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationAssignmentService {

    private final UserCertificationAssignmentRepository assignmentRepository;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final CertificationCatalogService catalogService;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final TokenService tokenService;
    private final OrganizationAccessService organizationAccessService;
    private final ApplicationEventPublisher eventPublisher;

    private static boolean isAssignableDefinition(TenantCertDefinition definition) {
        return definition != null && LifecycleFields.isCatalogSelectable(definition);
    }

    @Transactional(readOnly = true)
    public boolean isDefinitionAssignedToUser(Long userId, Long tenantCertDefinitionId) {
        if (tenantCertDefinitionId == null) {
            return false;
        }
        LocalDate today = OrganizationWallClockTime.today();
        for (UserCertificationAssignment assignment : assignmentRepository.findByUserId(userId)) {
            if (!assignment.isActiveOn(today)) {
                continue;
            }
            if (tenantCertDefinitionId.equals(assignment.getTenantCertDefinitionId())) {
                TenantCertDefinition definition =
                        tenantDefinitionRepository.findById(tenantCertDefinitionId).orElse(null);
                return isAssignableDefinition(definition);
            }
        }
        return false;
    }

    /**
     * Active catalog definitions for the user's membership organization that are not yet assigned.
     * Used by self-service certification submission (Nachweis einreichen) when no profile exists
     * yet.
     */
    @Transactional(readOnly = true)
    public List<CertificationDefinitionDto> listRequestableDefinitionsForUser(Long userId) {
        Long organizationId = resolveMembershipOrganizationId(userId);
        return catalogService.getCatalog(organizationId).getDefinitions().stream()
                .filter(CertificationDefinitionDto::isActive)
                .filter(definition -> !isDefinitionAssignedToUser(userId, definition.getId()))
                .sorted(
                        Comparator.comparing(
                                CertificationDefinitionDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserCertificationAssignmentDto> listAssignments(Long userId) {
        return assignmentRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageDto<UserCertificationAssignmentDto> pageAssignments(
            Long userId, int page, int size) {
        Pageable pageable = ListEndpointLimits.pageRequest(page, size);
        Page<UserCertificationAssignment> result =
                assignmentRepository.findByUserIdOrderByIdDesc(userId, pageable);
        return PageDto.from(result.map(this::toDto));
    }

    @Transactional
    public UserCertificationAssignmentDto createAssignment(
            Long callerOrganizationId,
            Long userId,
            CreateUserCertificationAssignmentRequest request) {
        Long tenantId = tokenService.requireTenantId();
        Long responsibleOrgId =
                request.getResponsibleOrganizationId() != null
                        ? request.getResponsibleOrganizationId()
                        : resolveMainOrganizationId(userId);

        assertCallerCanWriteResponsibleOrganization(callerOrganizationId, responsibleOrgId);

        TenantCertDefinition definition =
                requireAssignableDefinition(request.getTenantCertDefinitionId());
        catalogService.requireTypeAvailableForOrg(definition.getId(), responsibleOrgId);
        if (isDefinitionAssignedToUser(userId, definition.getId())) {
            throw new ConflictException("Definition already assigned to this user");
        }

        UserCertificationAssignment assignment =
                createAssignmentIfMissing(
                        tenantId,
                        userId,
                        definition.getId(),
                        responsibleOrgId,
                        CertificationAssignmentSource.MANUAL,
                        null,
                        null);
        if (assignment == null) {
            throw new ConflictException("Definition already assigned to this user");
        }
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());
        assignmentRepository.save(assignment);
        log.info(
                "Assigned certification definition {} to user {} responsible org {}",
                definition.getId(),
                userId,
                responsibleOrgId);
        eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        return toDto(assignment);
    }

    /**
     * Ensures assignments exist when a certification-record workflow is approved. Skips definitions
     * that are already assigned so approval remains idempotent.
     */
    @Transactional
    public void createAssignmentsFromWorkflow(
            Long tenantId, Long userId, Long workflowId, List<Long> tenantCertDefinitionIds) {
        createAssignments(
                tenantId,
                userId,
                resolveMembershipOrganizationId(userId),
                tenantCertDefinitionIds,
                CertificationAssignmentSource.WORKFLOW,
                AppLinkType.WORKFLOW,
                workflowId,
                true);
    }

    @Transactional
    public void createAssignmentsFromOnboarding(
            Long tenantId,
            Long userId,
            Long organizationId,
            Long invitationId,
            List<Long> tenantCertDefinitionIds) {
        createAssignments(
                tenantId,
                userId,
                organizationId,
                tenantCertDefinitionIds,
                CertificationAssignmentSource.ONBOARDING,
                AppLinkType.ONBOARDING_INVITATION,
                invitationId,
                false);
    }

    private void createAssignments(
            Long tenantId,
            Long userId,
            Long organizationId,
            List<Long> tenantCertDefinitionIds,
            CertificationAssignmentSource source,
            AppLinkType linkType,
            Long linkId,
            boolean verboseLogging) {
        if (tenantCertDefinitionIds == null || tenantCertDefinitionIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(tenantCertDefinitionIds);
        boolean createdAny = false;
        for (Long definitionId : uniqueIds) {
            TenantCertDefinition definition = requireAssignableDefinition(definitionId);
            catalogService.requireTypeAvailableForOrg(definition.getId(), organizationId);
            UserCertificationAssignment created =
                    createAssignmentIfMissing(
                            tenantId,
                            userId,
                            definition.getId(),
                            organizationId,
                            source,
                            linkType,
                            linkId);
            if (created != null) {
                createdAny = true;
                if (verboseLogging) {
                    log.info(
                            "Assigned certification definition {} to user {} via {} {}",
                            definition.getId(),
                            userId,
                            linkType.name().toLowerCase(),
                            linkId);
                }
            } else if (verboseLogging) {
                log.debug(
                        "Skipping already-assigned certification definition {} for user {} ({} {})",
                        definitionId,
                        userId,
                        linkType.name().toLowerCase(),
                        linkId);
            }
        }
        if (createdAny) {
            eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        }
    }

    @Transactional
    public void ensureAssignmentForEvent(
            Long tenantId,
            Long userId,
            Long tenantCertDefinitionId,
            Long organizationId,
            Long grantId) {
        if (isDefinitionAssignedToUser(userId, tenantCertDefinitionId)) {
            return;
        }
        TenantCertDefinition definition = requireAssignableDefinition(tenantCertDefinitionId);
        catalogService.requireTypeAvailableForOrg(definition.getId(), organizationId);
        UserCertificationAssignment created =
                createAssignmentIfMissing(
                        tenantId,
                        userId,
                        definition.getId(),
                        organizationId,
                        CertificationAssignmentSource.EVENT,
                        AppLinkType.CERTIFICATION_EVENT_GRANT,
                        grantId);
        if (created != null) {
            eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        }
    }

    private UserCertificationAssignment createAssignmentIfMissing(
            Long tenantId,
            Long userId,
            Long tenantCertDefinitionId,
            Long responsibleOrgId,
            CertificationAssignmentSource source,
            AppLinkType sourceAppLinkType,
            Long sourceAppLinkId) {
        if (isDefinitionAssignedToUser(userId, tenantCertDefinitionId)) {
            return null;
        }
        return assignmentRepository.save(
                UserCertificationAssignment.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .responsibleOrganizationId(responsibleOrgId)
                        .tenantCertDefinitionId(tenantCertDefinitionId)
                        .source(source)
                        .sourceAppLinkType(sourceAppLinkType)
                        .sourceAppLinkId(sourceAppLinkId)
                        .build());
    }

    @Transactional
    public void deleteAssignment(Long callerOrganizationId, Long userId, Long assignmentId) {
        UserCertificationAssignment assignment = requireAssignmentForUser(assignmentId, userId);
        assertCallerCanManageAssignment(callerOrganizationId, assignment);
        deleteAssignmentEntity(assignment);
    }

    @Transactional
    public void deleteAssignmentsForEventGrant(Long grantId) {
        for (UserCertificationAssignment assignment :
                assignmentRepository.findBySourceAndSourceAppLinkTypeAndSourceAppLinkId(
                        CertificationAssignmentSource.EVENT,
                        AppLinkType.CERTIFICATION_EVENT_GRANT,
                        grantId)) {
            deleteAssignmentEntity(assignment);
        }
    }

    /** Removes EVENT-sourced assignments for a grant whose users are no longer recipients. */
    @Transactional
    public void deleteOrphanAssignmentsForEventGrant(Long grantId, Set<Long> keepUserIds) {
        for (UserCertificationAssignment assignment :
                assignmentRepository.findBySourceAndSourceAppLinkTypeAndSourceAppLinkId(
                        CertificationAssignmentSource.EVENT,
                        AppLinkType.CERTIFICATION_EVENT_GRANT,
                        grantId)) {
            if (keepUserIds.contains(assignment.getUserId())) {
                continue;
            }
            deleteAssignmentEntity(assignment);
        }
    }

    private void deleteAssignmentEntity(UserCertificationAssignment assignment) {
        Long tenantId = assignment.getTenantId();
        Long userId = assignment.getUserId();
        assignmentRepository.delete(assignment);
        eventPublisher.publishEvent(new CertificationAssignmentChangedEvent(tenantId, userId));
        log.info("Removed certification assignment {} from user {}", assignment.getId(), userId);
    }

    private Long resolveMembershipOrganizationId(Long userId) {
        Instant now = Instant.now();
        return membershipRepository
                .findActiveByUser_IdAndMainTrue(userId, now)
                .or(() -> membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(userId, now))
                .map(membership -> membership.getOrganization().getId())
                .orElseThrow(
                        () -> new ConflictException("User has no active organization membership"));
    }

    private Long resolveMainOrganizationId(Long userId) {
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, Instant.now())
                        .or(() -> membershipRepository.findByUser_IdAndMainTrue(userId))
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "User has no main organization; provide responsibleOrganizationId explicitly"));
        return membership.getOrganization().getId();
    }

    private TenantCertDefinition requireAssignableDefinition(Long tenantCertDefinitionId) {
        TenantCertDefinition definition = requireDefinition(tenantCertDefinitionId);
        if (definition.isArchived()) {
            throw new ConflictException("Certification definition is archived");
        }
        if (!definition.isActive()) {
            throw new ConflictException("Certification definition is not active");
        }
        return definition;
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

    private UserCertificationAssignment requireAssignmentForUser(Long assignmentId, Long userId) {
        return assignmentRepository
                .findByIdAndUserId(assignmentId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Certification assignment not found: " + assignmentId));
    }

    private void assertCallerCanManageAssignment(
            Long callerOrganizationId, UserCertificationAssignment assignment) {
        Long responsibleOrgId = assignment.getResponsibleOrganizationId();
        if (responsibleOrgId != null
                && !responsibleOrgId.equals(callerOrganizationId)
                && !hasCertificationWrite(responsibleOrgId)) {
            throw new AccessDeniedException(
                    "No write permission for the assignment's responsible organization");
        }
    }

    private void assertCallerCanWriteResponsibleOrganization(
            Long callerOrganizationId, Long responsibleOrgId) {
        if (responsibleOrgId == null) {
            throw new ConflictException("responsibleOrganizationId is required");
        }
        if (responsibleOrgId.equals(callerOrganizationId)) {
            return;
        }
        if (!hasCertificationWrite(responsibleOrgId)) {
            throw new AccessDeniedException(
                    "No write permission for the target responsible organization");
        }
    }

    private boolean hasCertificationWrite(Long organizationId) {
        return organizationAccessService.hasOrganizationPermission(
                organizationId, PermissionType.CERTIFICATION_WRITE);
    }

    private UserCertificationAssignmentDto toDto(UserCertificationAssignment assignment) {
        String definitionName =
                tenantDefinitionRepository
                        .findById(assignment.getTenantCertDefinitionId())
                        .map(TenantCertDefinition::getName)
                        .orElse(null);
        return UserCertificationAssignmentDto.builder()
                .id(assignment.getId())
                .userId(assignment.getUserId())
                .responsibleOrganizationId(assignment.getResponsibleOrganizationId())
                .tenantCertDefinitionId(assignment.getTenantCertDefinitionId())
                .certDefinitionName(definitionName)
                .source(assignment.getSource() == null ? null : assignment.getSource().name())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .build();
    }
}
