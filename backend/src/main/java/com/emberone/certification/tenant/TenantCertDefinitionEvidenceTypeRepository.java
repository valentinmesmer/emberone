/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.tenant;

import com.emberone.certification.domain.CertEvidenceType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface TenantCertDefinitionEvidenceTypeRepository
        extends JpaRepository<TenantCertDefinitionEvidenceType, Long> {

    List<TenantCertDefinitionEvidenceType> findByTenantCertDefinitionId(
            Long tenantCertDefinitionId);

    @Transactional
    void deleteByTenantCertDefinitionIdAndEvidenceTypeIn(
            Long tenantCertDefinitionId, Collection<CertEvidenceType> evidenceTypes);
}
