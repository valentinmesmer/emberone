/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.organization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface OrganizationCourseOfferingTypeRepository
        extends JpaRepository<OrganizationCourseOfferingType, Long> {

    List<OrganizationCourseOfferingType> findByOrganizationId(Long organizationId);

    List<OrganizationCourseOfferingType> findByDefinitionConfigKey(String definitionConfigKey);

    @Transactional
    void deleteByDefinitionConfigKey(String definitionConfigKey);
}
