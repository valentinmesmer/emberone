/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface OrganizationOnboardingDocumentDefinitionRepository
        extends JpaRepository<OrganizationOnboardingDocumentDefinition, Long> {

    List<OrganizationOnboardingDocumentDefinition> findByOrganizationIdOrderByNameAsc(
            Long organizationId);

    List<OrganizationOnboardingDocumentDefinition>
            findByOrganizationIdAndActiveTrueAndArchivedFalseOrderByNameAsc(Long organizationId);

    Optional<OrganizationOnboardingDocumentDefinition> findByOrganizationIdAndConfigKey(
            Long organizationId, String configKey);

    List<OrganizationOnboardingDocumentDefinition>
            findByOrganizationIdAndConfigKeyInAndArchivedFalse(
                    Long organizationId, Collection<String> configKeys);

    boolean existsByOrganizationIdAndConfigKeyAndArchivedFalse(
            Long organizationId, String configKey);

    boolean existsByOrganizationIdAndConfigKeyAndArchivedFalseAndIdNot(
            Long organizationId, String configKey, Long id);
}
