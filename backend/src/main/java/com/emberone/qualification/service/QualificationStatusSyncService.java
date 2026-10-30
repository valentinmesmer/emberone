/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.domain.QualificationOrgDecisionStatus;
import com.emberone.qualification.domain.QualificationRevokeReason;
import com.emberone.qualification.dto.QualificationDefinitionDto;
import com.emberone.qualification.dto.QualificationStatusItemDto;
import com.emberone.qualification.dto.UserQualificationStatusDto;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QualificationStatusSyncService {

    private final QualificationStatusService statusService;
    private final QualificationCatalogService catalogService;
    private final QualificationOrgDecisionService orgDecisionService;
    private final TenantQualificationDefinitionRepository definitionRepository;
    private final UserQualificationOrgDecisionRepository orgDecisionRepository;
    private final UserOrganizationMembershipRepository membershipRepository;

    @Transactional
    public void syncForUser(Long tenantId, Long userId) {
        TenantScopeGuard.bind(tenantId);
        List<UserOrganizationMembership> memberships =
                membershipRepository.findActiveByUser_Id(userId, Instant.now());
        for (UserOrganizationMembership membership : memberships) {
            syncForUserInOrganization(tenantId, userId, membership.getOrganization().getId());
        }
    }

    @Transactional
    public void syncForUserInOrganization(Long tenantId, Long userId, Long organizationId) {
        UserQualificationStatusDto status = statusService.getStatusForUser(organizationId, userId);
        for (QualificationStatusItemDto item : status.getItems()) {
            if (item.isOrgDecisionGranted() && !item.isPrerequisitesMet()) {
                orgDecisionService.autoRevoke(
                        tenantId,
                        userId,
                        item.getTenantQualificationDefinitionId(),
                        organizationId,
                        QualificationRevokeReason.PREREQUISITES_LOST);
            }
        }
    }

    @Transactional
    public void syncForQualification(Long tenantId, Long qualificationDefinitionId) {
        TenantScopeGuard.bind(tenantId);
        if (definitionRepository.findById(qualificationDefinitionId).isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        orgDecisionRepository
                .findByTenantQualificationDefinitionId(qualificationDefinitionId)
                .forEach(decision -> userIds.add(decision.getUserId()));
        for (Long userId : userIds) {
            syncForUser(tenantId, userId);
        }
    }

    @Transactional
    public void syncForCatalogChange(Long tenantId, List<Long> organizationIds) {
        TenantScopeGuard.bind(tenantId);
        for (Long organizationId : organizationIds) {
            Set<Long> availableIds =
                    catalogService.resolveCatalogForOrganization(organizationId).stream()
                            .map(QualificationDefinitionDto::getId)
                            .collect(Collectors.toSet());
            orgDecisionRepository
                    .findByOrganizationId(organizationId)
                    .forEach(
                            decision -> {
                                if (!availableIds.contains(
                                                decision.getTenantQualificationDefinitionId())
                                        && decision.getStatus()
                                                == QualificationOrgDecisionStatus.GRANTED) {
                                    orgDecisionService.autoRevoke(
                                            tenantId,
                                            decision.getUserId(),
                                            decision.getTenantQualificationDefinitionId(),
                                            organizationId,
                                            QualificationRevokeReason.REMOVED_FROM_CATALOG);
                                }
                            });
        }
    }
}
