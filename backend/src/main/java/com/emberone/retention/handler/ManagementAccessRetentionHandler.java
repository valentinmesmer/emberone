/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.ManagementActivationRetentionRepository;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Activations are kept for the audit trail until twelve months after they ended or expired. */
@Component
@RequiredArgsConstructor
public class ManagementAccessRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES = Set.of("org_management_activations");

    private final ManagementActivationRetentionRepository activationRetentionRepository;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.MANAGEMENT_ACCESS;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        return RetentionBatchDeletes.deleteUntilExhausted(
                () -> activationRetentionRepository.deleteActivationsEndedBefore(cutoff));
    }
}
