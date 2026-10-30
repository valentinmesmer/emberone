/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.repository;

import com.emberone.certification.domain.CertificationAssignmentSource;
import com.emberone.certification.domain.UserCertificationAssignment;
import com.emberone.platform.navigation.domain.AppLinkType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserCertificationAssignmentRepository
        extends JpaRepository<UserCertificationAssignment, Long> {

    List<UserCertificationAssignment> findByUserId(Long userId);

    Page<UserCertificationAssignment> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    @Query("SELECT DISTINCT a.userId FROM UserCertificationAssignment a ORDER BY a.userId")
    Page<Long> findDistinctUserIds(Pageable pageable);

    List<UserCertificationAssignment> findByUserIdIn(Collection<Long> userIds);

    Optional<UserCertificationAssignment> findByIdAndUserId(Long id, Long userId);

    Optional<UserCertificationAssignment> findByUserIdAndTenantCertDefinitionId(
            Long userId, Long tenantCertDefinitionId);

    boolean existsByTenantCertDefinitionIdIn(Collection<Long> tenantCertDefinitionIds);

    List<UserCertificationAssignment> findBySourceAndSourceAppLinkTypeAndSourceAppLinkId(
            CertificationAssignmentSource source,
            AppLinkType sourceAppLinkType,
            Long sourceAppLinkId);
}
