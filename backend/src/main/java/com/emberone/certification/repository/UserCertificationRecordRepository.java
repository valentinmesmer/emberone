/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.repository;

import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.platform.navigation.domain.AppLinkType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserCertificationRecordRepository
        extends JpaRepository<UserCertificationRecord, Long> {

    List<UserCertificationRecord> findByUserId(Long userId);

    Page<UserCertificationRecord> findByUserIdOrderByCompletedDateDescIdDesc(
            Long userId, Pageable pageable);

    List<UserCertificationRecord> findByUserIdIn(Collection<Long> userIds);

    List<UserCertificationRecord> findByUserIdAndTenantCertDefinitionId(
            Long userId, Long tenantCertDefinitionId);

    List<UserCertificationRecord> findAllByAppLinkTypeAndAppLinkId(
            AppLinkType appLinkType, Long appLinkId);

    boolean existsByTenantCertDefinitionIdIn(Collection<Long> tenantCertDefinitionIds);
}
