/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.AbsenceRetentionRepository;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AbsenceRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES = Set.of("user_absences", "user_absence_organizations");

    private final AbsenceRetentionRepository absenceRetentionRepository;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.ABSENCE;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        return RetentionBatchDeletes.deleteUntilExhausted(
                () -> absenceRetentionRepository.deleteAbsencesEndingBefore(cutoff));
    }
}
