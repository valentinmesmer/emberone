/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.repository;

import com.emberone.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Explicit tenant filter: validates a caller-supplied tenant id (NotificationService); users
    // has the global-admin read policy.
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    boolean existsByUsername(String username);

    boolean existsByIdAndDeactivatedAtIsNull(Long id);
}
