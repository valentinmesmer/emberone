/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.qualification.domain.*;
import com.emberone.qualification.dto.CreateQualificationOrgDecisionRequest;
import com.emberone.qualification.dto.UserQualificationOrgDecisionDto;
import com.emberone.qualification.event.QualificationOrgDecisionChangedEvent;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.tenant.TenantQualificationDefinition;
import com.emberone.qualification.util.QualificationOrgDecisionIntervals;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualificationOrgDecisionService {

    private final UserQualificationOrgDecisionRepository orgDecisionRepository;
    private final TenantQualificationDefinitionService definitionService;
    private final QualificationCatalogService catalogService;
    private final QualificationStatusService statusService;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    static UserQualificationOrgDecisionDto toDto(UserQualificationOrgDecision decision) {
        return UserQualificationOrgDecisionDto.builder()
                .id(decision.getId())
                .status(decision.getStatus())
                .startDate(decision.getStartDate())
                .endDate(decision.getEndDate())
                .notes(decision.getNotes())
                .build();
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Transactional
    public UserQualificationOrgDecisionDto grant(
            Long organizationId,
            Long userId,
            String qualificationConfigKey,
            CreateQualificationOrgDecisionRequest request) {
        if (request.getId() != null) {
            throw new ConflictException(
                    "Use PUT to update an existing organizational decision interval");
        }
        return persistGrant(organizationId, userId, qualificationConfigKey, request, null);
    }

    @Transactional
    public UserQualificationOrgDecisionDto updateGrant(
            Long organizationId,
            Long userId,
            String qualificationConfigKey,
            CreateQualificationOrgDecisionRequest request) {
        if (request.getId() == null) {
            throw new ConflictException("Decision id is required to update an interval");
        }
        return persistGrant(
                organizationId, userId, qualificationConfigKey, request, request.getId());
    }

    @Transactional
    public void autoRevoke(
            Long tenantId,
            Long userId,
            Long qualificationDefinitionId,
            Long organizationId,
            QualificationRevokeReason reason) {
        LocalDate today = OrganizationWallClockTime.today();
        listDecisions(userId, qualificationDefinitionId, organizationId).stream()
                .filter(decision -> decision.getStatus() == QualificationOrgDecisionStatus.GRANTED)
                .filter(decision -> decision.isActiveGrantOn(today))
                .forEach(
                        decision -> {
                            decision.setStatus(QualificationOrgDecisionStatus.REVOKED);
                            decision.setRevokeReason(reason);
                            decision.setRevokedAt(Instant.now());
                            orgDecisionRepository.save(decision);
                            log.info(
                                    "Revoked qualification org decision {} for user {} ({})",
                                    decision.getId(),
                                    userId,
                                    reason);
                            eventPublisher.publishEvent(
                                    new QualificationOrgDecisionChangedEvent(
                                            tenantId, userId, qualificationDefinitionId));
                        });
    }

    private UserQualificationOrgDecisionDto persistGrant(
            Long organizationId,
            Long userId,
            String qualificationConfigKey,
            CreateQualificationOrgDecisionRequest request,
            Long updateDecisionId) {
        Long tenantId = tokenService.requireTenantId();
        Long decidedByUserId = tokenService.getReferencedUserId();
        TenantQualificationDefinition definition =
                definitionService.requireActiveDefinition(qualificationConfigKey);
        if (!definition.isRequiresOrgDecision()) {
            throw new ConflictException(
                    "Qualification does not require an organizational decision");
        }
        catalogService.requireAvailableForOrg(definition.getId(), organizationId);
        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, organizationId, userId);
        if (status == QualificationExerciseStatus.PREREQUISITES_MISSING) {
            throw new ConflictException("Prerequisites are not met for this qualification");
        }
        QualificationOrgDecisionIntervals.validateDateRange(
                request.getStartDate(), request.getEndDate());

        List<UserQualificationOrgDecision> existing =
                listDecisions(userId, definition.getId(), organizationId);
        if (QualificationOrgDecisionIntervals.hasOverlappingGrantedInterval(
                existing, request.getStartDate(), request.getEndDate(), updateDecisionId)) {
            throw new ConflictException(
                    "Organizational decision intervals must not overlap for the same organization");
        }

        UserQualificationOrgDecision decision;
        if (updateDecisionId != null) {
            decision =
                    existing.stream()
                            .filter(item -> updateDecisionId.equals(item.getId()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Qualification org decision not found"));
            if (decision.getStatus() != QualificationOrgDecisionStatus.GRANTED) {
                throw new ConflictException("Only granted intervals can be updated");
            }
        } else {
            decision =
                    UserQualificationOrgDecision.builder()
                            .tenantId(tenantId)
                            .userId(userId)
                            .tenantQualificationDefinitionId(definition.getId())
                            .organizationId(organizationId)
                            .build();
        }

        decision.setStatus(QualificationOrgDecisionStatus.GRANTED);
        decision.setDecidedByUserId(decidedByUserId);
        decision.setDecidedAt(Instant.now());
        decision.setStartDate(request.getStartDate());
        decision.setEndDate(request.getEndDate());
        decision.setNotes(normalize(request.getNotes()));
        decision.setRevokeReason(null);
        decision.setRevokedAt(null);
        UserQualificationOrgDecision saved = orgDecisionRepository.save(decision);
        if (updateDecisionId != null) {
            log.info(
                    "Updated qualification org decision {} for user {} in organization {}",
                    saved.getId(),
                    userId,
                    organizationId);
        } else {
            log.info(
                    "Granted qualification {} for user {} in organization {}",
                    definition.getId(),
                    userId,
                    organizationId);
        }
        eventPublisher.publishEvent(
                new QualificationOrgDecisionChangedEvent(tenantId, userId, definition.getId()));
        return toDto(saved);
    }

    List<UserQualificationOrgDecision> listDecisions(
            Long userId, Long definitionId, Long organizationId) {
        return orgDecisionRepository
                .findByUserIdAndTenantQualificationDefinitionIdAndOrganizationIdOrderByStartDateDesc(
                        userId, definitionId, organizationId);
    }
}
