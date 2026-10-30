/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import com.emberone.platform.security.PostgresSessionContext;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.RetentionPolicyService;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.domain.RetentionRun;
import com.emberone.retention.profile.RetentionPeriod;
import com.emberone.retention.profile.RetentionProfile;
import com.emberone.retention.repository.RetentionRunRepository;
import com.emberone.retention.spi.RetentionHandler;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RetentionHandlerInvoker {

    private final RetentionPolicyService retentionPolicyService;
    private final RetentionRunRepository retentionRunRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int invoke(RetentionHandler handler) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for retention");
        }
        enableRetentionMode();
        RetentionProfile profile = retentionPolicyService.profileForTenant(tenantId);
        RetentionCategory category = handler.category();
        RetentionPeriod period = profile.period(category);
        Instant cutoff = period.cutoffBefore(Instant.now());
        int deleted = handler.apply(cutoff);
        if (deleted > 0) {
            retentionRunRepository.save(
                    RetentionRun.builder()
                            .tenantId(tenantId)
                            .category(category)
                            .profileKey(profile.key())
                            .periodLabel(period.label())
                            .deletedCount(deleted)
                            .ranAt(Instant.now())
                            .build());
        }
        return deleted;
    }

    private void enableRetentionMode() {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(
                connection -> {
                    try {
                        PostgresSessionContext.setRetention(connection, true);
                    } catch (SQLException ex) {
                        throw new IllegalStateException(
                                "Failed to set PostgreSQL retention session flag", ex);
                    }
                });
    }
}
