/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface FunctionGrantRepository extends JpaRepository<FunctionGrant, Long> {

    List<FunctionGrant> findByStandardId(Long standardId);

    Optional<FunctionGrant> findByStandardIdAndOrganizationGroupConfigKeyAndMembershipType(
            Long standardId, String organizationGroupConfigKey, GroupMembershipType membershipType);
}
