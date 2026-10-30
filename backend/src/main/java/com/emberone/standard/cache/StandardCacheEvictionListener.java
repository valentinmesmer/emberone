/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.standard.event.OrganizationStandardOverrideChangedEvent;
import com.emberone.standard.event.OrganizationTypeStandardAssignedEvent;
import com.emberone.standard.event.StandardEnabledModulesChangedEvent;
import com.emberone.standard.repository.StandardAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StandardCacheEvictionListener {

    private final OrganizationRepository organizationRepository;
    private final StandardAssignmentRepository assignmentRepository;
    private final EffectiveStandardOrganizationCacheEvictor
            effectiveStandardOrganizationCacheEvictor;
    private final EffectiveStandardTypeCache effectiveStandardTypeCache;
    private final StandardOrganizationIdCache standardOrganizationIdCache;
    private final EnabledModulesCacheEvictor enabledModulesCacheEvictor;

    @AfterCommitEventListener(readOnly = true)
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        if (event.tenantId() == null || event.organizationId() == null) {
            return;
        }
        effectiveStandardOrganizationCacheEvictor.evict(event.tenantId(), event.organizationId());
        organizationRepository
                .findById(event.organizationId())
                .ifPresent(
                        org -> {
                            if (org.getStandardId() != null) {
                                evictOrganizationsForStandard(
                                        event.tenantId(), org.getStandardId());
                            }
                        });
        if (event.organizationTypeId() != null) {
            assignmentRepository
                    .findByTenantIdAndOrganizationType_Id(
                            event.tenantId(), event.organizationTypeId())
                    .ifPresent(
                            assignment ->
                                    evictOrganizationsForStandard(
                                            event.tenantId(), assignment.getStandardId()));
        }
    }

    @AfterCommitEventListener(readOnly = true)
    public void onOrganizationTypeStandardAssigned(OrganizationTypeStandardAssignedEvent event) {
        if (event.tenantId() == null || event.organizationTypeId() == null) {
            return;
        }
        effectiveStandardTypeCache.evict(event.tenantId(), event.organizationTypeId());
        evictOrganizationsForStandard(event.tenantId(), event.previousStandardId());
        evictOrganizationsForStandard(event.tenantId(), event.newStandardId());
        organizationRepository.findByType_IdAndStandardIdIsNull(event.organizationTypeId()).stream()
                .filter(org -> event.tenantId().equals(org.getTenantId()))
                .forEach(
                        org ->
                                effectiveStandardOrganizationCacheEvictor.evict(
                                        event.tenantId(), org.getId()));
    }

    @AfterCommitEventListener(readOnly = true)
    public void onOrganizationStandardOverrideChanged(
            OrganizationStandardOverrideChangedEvent event) {
        if (event.tenantId() == null || event.organizationId() == null) {
            return;
        }
        effectiveStandardOrganizationCacheEvictor.evict(event.tenantId(), event.organizationId());
        evictOrganizationsForStandard(event.tenantId(), event.previousStandardId());
        evictOrganizationsForStandard(event.tenantId(), event.newStandardId());
    }

    @AfterCommitCallback
    public void onStandardEnabledModulesChanged(StandardEnabledModulesChangedEvent event) {
        if (event.tenantId() == null || event.standardId() == null) {
            return;
        }
        enabledModulesCacheEvictor.evict(event.tenantId(), event.standardId());
    }

    private void evictOrganizationsForStandard(Long tenantId, Long standardId) {
        if (tenantId != null && standardId != null) {
            standardOrganizationIdCache.evict(tenantId, standardId);
        }
    }
}
