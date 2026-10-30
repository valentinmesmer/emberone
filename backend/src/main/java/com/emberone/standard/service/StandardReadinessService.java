/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardAssignment;
import com.emberone.standard.exception.StandardNotConfiguredException;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.repository.StandardRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks whether a tenant has cloned and assigned custom standards for every active organization
 * type (the tenant's own {@link OrganizationTypeDefinition} catalog) before organizations may be
 * created.
 */
@Service
@RequiredArgsConstructor
public class StandardReadinessService {

    private final StandardAssignmentRepository assignmentRepository;
    private final StandardRepository standardRepository;
    private final OrganizationTypeDefinitionService organizationTypeDefinitionService;

    @Transactional(readOnly = true)
    public boolean isReadyForOrganizationCreation(long tenantId) {
        return organizationTypeDefinitionService.listActive().stream()
                .allMatch(type -> isTypeReady(tenantId, type.getId()));
    }

    public void requireReadyForOrganizationCreation(long tenantId, Long organizationTypeId) {
        if (!isTypeReady(tenantId, organizationTypeId)) {
            throw new StandardNotConfiguredException();
        }
    }

    private boolean isTypeReady(long tenantId, Long organizationTypeId) {
        return resolveAssignedCustomStandard(tenantId, organizationTypeId).isPresent();
    }

    private Optional<Standard> resolveAssignedCustomStandard(
            long tenantId, Long organizationTypeId) {
        Optional<StandardAssignment> assignment =
                assignmentRepository.findByTenantIdAndOrganizationType_Id(
                        tenantId, organizationTypeId);
        if (assignment.isEmpty()) {
            return Optional.empty();
        }
        return standardRepository
                .findById(assignment.get().getStandardId())
                .filter(
                        standard ->
                                standard.getTenantId() != null
                                        && standard.getTenantId().equals(tenantId))
                .filter(standard -> !standard.isSystem());
    }
}
