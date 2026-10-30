/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.repository;

import com.emberone.qualification.domain.UserQualificationOrgDecision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserQualificationOrgDecisionRepository
        extends JpaRepository<UserQualificationOrgDecision, Long> {

    List<UserQualificationOrgDecision>
            findByUserIdAndTenantQualificationDefinitionIdAndOrganizationIdOrderByStartDateDesc(
                    Long userId, Long tenantQualificationDefinitionId, Long organizationId);

    List<UserQualificationOrgDecision> findByUserId(Long userId);

    List<UserQualificationOrgDecision> findByTenantQualificationDefinitionId(
            Long tenantQualificationDefinitionId);

    List<UserQualificationOrgDecision> findByOrganizationId(Long organizationId);

    boolean existsByTenantQualificationDefinitionIdIn(List<Long> definitionIds);
}
