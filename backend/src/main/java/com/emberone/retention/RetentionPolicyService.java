/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention;

import com.emberone.federalstate.FederalStateService;
import com.emberone.retention.profile.RetentionProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RetentionPolicyService {

    private final FederalStateService federalStateService;

    @Transactional(readOnly = true)
    public RetentionProfile profileForTenant(Long tenantId) {
        return federalStateService.settings(tenantId).baselines().retentionProfile();
    }
}
