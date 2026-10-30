/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.standard;

import com.emberone.organizationgroup.domain.OrganizationGroupType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface OrgGroupTemplateRepository extends JpaRepository<OrgGroupTemplate, Long> {

    List<OrgGroupTemplate> findByStandardId(Long standardId);

    Optional<OrgGroupTemplate> findByStandardIdAndConfigKey(Long standardId, String configKey);

    boolean existsByStandardIdAndConfigKey(Long standardId, String configKey);

    boolean existsByStandardIdAndGroupType(Long standardId, OrganizationGroupType groupType);
}
