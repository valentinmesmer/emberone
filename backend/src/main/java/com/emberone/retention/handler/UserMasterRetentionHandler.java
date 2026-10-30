/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.UserMasterRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMasterRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of(
                    "users",
                    "idm_users",
                    "user_organization_memberships",
                    "user_organization_group_memberships",
                    "cert_user_assignments",
                    "cert_event_grant_recipients",
                    "training_course_user_submissions",
                    "training_course_user_submission_documents",
                    "qual_user_org_decisions",
                    "usr_entry_document_fulfillments",
                    "usr_entry_document_fulfillment_events",
                    "user_ranks",
                    "course_registrations");

    private final UserMasterRetentionRepository userMasterRetentionRepository;
    private final RetentionObjectStorageSupport retentionObjectStorageSupport;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.USER_MASTER;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for retention");
        }
        List<Long> userIds = userMasterRetentionRepository.findDeactivatedUserIdsBefore(cutoff);
        for (Long userId : userIds) {
            retentionObjectStorageSupport.enqueueUserStoragePrefix(tenantId, userId);
        }
        return RetentionBatchDeletes.deleteUntilExhausted(
                () -> userMasterRetentionRepository.deleteDeactivatedUsersBefore(cutoff));
    }
}
