/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.tenant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantOnboardingDocumentDefinitionTemplateVersionRepository
        extends JpaRepository<TenantOnboardingDocumentDefinitionTemplateVersion, Long> {

    Optional<TenantOnboardingDocumentDefinitionTemplateVersion>
            findByDocumentDefinitionIdAndVersion(long documentDefinitionId, int version);

    List<TenantOnboardingDocumentDefinitionTemplateVersion>
            findByDocumentDefinitionIdOrderByVersionAsc(long documentDefinitionId);

    @Query(
            """
            SELECT COALESCE(MAX(v.version), 0) FROM TenantOnboardingDocumentDefinitionTemplateVersion v
            WHERE v.documentDefinitionId = :documentDefinitionId
            """)
    int findMaxVersion(@Param("documentDefinitionId") long documentDefinitionId);

    boolean existsByDocumentDefinitionIdAndVersion(long documentDefinitionId, int version);

    List<TenantOnboardingDocumentDefinitionTemplateVersion> findByDocumentDefinitionIdIn(
            Collection<Long> documentDefinitionIds);
}
