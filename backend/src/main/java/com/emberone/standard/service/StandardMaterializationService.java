/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.admin.standard.dto.StandardSwitchImpactDto;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.standard.OrganizationGroupStandardDomain;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.repository.StandardRepository;
import com.emberone.standard.spi.StandardDomainRegistry;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializes standard templates into per-org instance rows and re-materializes them when a
 * standard assignment changes. Replaces the hard-coded per-org configuration that used to live in
 * {@code OrganizationService.afterOrganizationCreate}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandardMaterializationService {

    private final StandardDomainRegistry registry;
    private final StandardRepository standardRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationGroupStandardDomain organizationGroupStandardDomain;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final StandardOrganizationResolver organizationResolver;

    /**
     * Materialize the organization's effective standard (per-org override, else its type's
     * assignment) into fresh per-org instance rows.
     *
     * <p>Resolves from the organization entity / type assignment directly - not via the effective
     * org-standard cache - so create/reassign in the same transaction as an override change cannot
     * rematerialize a stale cached standard.
     */
    @Transactional
    public void materializeForOrg(Organization organization) {
        Optional<Standard> standard = resolveTargetStandard(organization);
        if (standard.isEmpty()) {
            log.warn(
                    "No standard resolved for organization {} (type {}); created without materialized config",
                    organization.getId(),
                    organization.getType().getId());
            return;
        }
        log.info(
                "Materializing standard {} for organization {}",
                standard.get().getId(),
                organization.getId());
        registry.materializeForOrg(organization.getId(), standard.get());
    }

    /**
     * Re-materialize every existing organization of {@code organizationTypeId} that inherits its
     * type's standard (organizations with an explicit per-org override are left untouched) after
     * the type's assigned standard changed. The caller passes the new standard explicitly so this
     * path does not depend on the (still-cached) type assignment.
     */
    @Transactional
    public void reassignStandardForType(Long organizationTypeId, Standard standard) {
        List<Organization> organizations =
                organizationRepository.findByType_IdAndStandardIdIsNull(organizationTypeId);
        log.info(
                "Re-materializing standard {} for {} inheriting organization(s) of type {}",
                standard.getId(),
                organizations.size(),
                organizationTypeId);
        for (Organization organization : organizations) {
            registry.reassignForOrg(organization.getId(), standard);
        }
    }

    /**
     * Re-materialize a single organization against {@code standard}. Used when a per-org standard
     * override is set or cleared - the caller must pass the new effective standard explicitly (same
     * pattern as {@link #reassignStandardForType}), because the effective org-standard cache is
     * only evicted after commit.
     */
    @Transactional
    public void reassignStandardForOrg(Organization organization, Standard standard) {
        log.info(
                "Re-materializing standard {} for organization {}",
                standard.getId(),
                organization.getId());
        registry.reassignForOrg(organization.getId(), standard);
    }

    /**
     * Resolve the standard that should drive materialization from the live organization row / type
     * assignment, bypassing {@link EffectiveStandardResolver}'s org cache.
     */
    private Optional<Standard> resolveTargetStandard(Organization organization) {
        if (organization.getStandardId() != null) {
            return standardRepository
                    .findById(organization.getStandardId())
                    .filter(standard -> organization.getTenantId().equals(standard.getTenantId()));
        }
        if (organization.getType() == null || organization.getType().getId() == null) {
            return Optional.empty();
        }
        return effectiveStandardResolver.resolveForOrganizationType(organization.getType().getId());
    }

    /**
     * Resolve every existing organization currently driven by this standard. Used to propagate
     * template <em>edits</em> onto the organizations that materialized from the standard:
     * organizations of every org type assigned this standard <em>that inherit</em> (no override),
     * plus organizations whose explicit per-org override points at this standard.
     */
    @Transactional(readOnly = true)
    public List<Organization> organizationsForStandard(Standard standard) {
        return organizationResolver.resolveOrganizationsForStandard(standard);
    }

    @Transactional(readOnly = true)
    public StandardSwitchImpactDto previewReassignForType(
            Long organizationTypeId, Standard newStandard) {
        Optional<Standard> current =
                effectiveStandardResolver.resolveForOrganizationType(organizationTypeId);
        if (current.isEmpty() || current.get().getId().equals(newStandard.getId())) {
            return StandardSwitchImpactDto.builder().removedGroups(List.of()).build();
        }
        List<Organization> orgs =
                organizationRepository.findByType_IdAndStandardIdIsNull(organizationTypeId);
        return StandardSwitchImpactDto.builder()
                .removedGroups(
                        organizationGroupStandardDomain.removedConfigKeysImpact(
                                current.get(), newStandard, orgs))
                .build();
    }

    @Transactional(readOnly = true)
    public StandardSwitchImpactDto previewReassignForOrg(
            Organization organization, Standard newStandard) {
        Optional<Standard> current = effectiveStandardResolver.resolveForOrganization(organization);
        if (current.isEmpty() || current.get().getId().equals(newStandard.getId())) {
            return StandardSwitchImpactDto.builder().removedGroups(List.of()).build();
        }
        return StandardSwitchImpactDto.builder()
                .removedGroups(
                        organizationGroupStandardDomain.removedConfigKeysImpact(
                                current.get(), newStandard, List.of(organization)))
                .build();
    }
}
