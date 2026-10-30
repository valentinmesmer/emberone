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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface OrganizationOnboardingDocumentDefinitionTemplateVersionRepository
        extends JpaRepository<OrganizationOnboardingDocumentDefinitionTemplateVersion, Long> {

    Optional<OrganizationOnboardingDocumentDefinitionTemplateVersion>
            findByOrgDocumentDefinitionIdAndVersion(long orgDocumentDefinitionId, int version);

    List<OrganizationOnboardingDocumentDefinitionTemplateVersion>
            findByOrgDocumentDefinitionIdOrderByVersionAsc(long orgDocumentDefinitionId);

    @Query(
            """
            SELECT COALESCE(MAX(v.version), 0) FROM OrganizationOnboardingDocumentDefinitionTemplateVersion v
            WHERE v.orgDocumentDefinitionId = :orgDocumentDefinitionId
            """)
    int findMaxVersion(@Param("orgDocumentDefinitionId") long orgDocumentDefinitionId);

    boolean existsByOrgDocumentDefinitionIdAndVersion(long orgDocumentDefinitionId, int version);

    List<OrganizationOnboardingDocumentDefinitionTemplateVersion> findByOrgDocumentDefinitionIdIn(
            Collection<Long> orgDocumentDefinitionIds);
}
