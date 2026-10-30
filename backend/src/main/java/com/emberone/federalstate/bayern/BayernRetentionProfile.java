/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.bayern;

import static com.emberone.retention.domain.RetentionCategory.*;
import static com.emberone.retention.profile.RetentionPeriod.*;
import static com.emberone.retention.profile.RetentionProfile.period;

import com.emberone.retention.profile.RetentionProfile;

/**
 * Bavaria retention profile {@code BY} (placeholder periods, DSB approval pending).
 *
 * <p>Operators must review and approve retention periods before production use; the shipped
 * defaults are for development and documentation only. Retention runs on the nightly scheduler
 * ({@link com.emberone.retention.schedule.RetentionScheduler}).
 *
 * <p>The key is persisted in {@code retention_runs.profile_key} and must stay stable; a later
 * additional Bavaria profile gets a new key (e.g. {@code BY_2}).
 */
public final class BayernRetentionProfile {

    public static final String KEY = "BY";

    public static final RetentionProfile PROFILE =
            RetentionProfile.of(
                    KEY,
                    period(EXIT_IMMEDIATE, ofDays(0)),
                    period(EXIT_SETTLEMENT, ofMonths(6)),
                    period(DRIVING_LICENSE, ofYears(3)),
                    period(EVIDENCE, ofMonths(12)),
                    period(ABSENCE, ofMonths(6)),
                    period(TIME_ENTRY, ofYears(5)),
                    period(EVENT, ofYears(5)),
                    period(APPOINTMENT, ofMonths(12)),
                    period(WORKFLOW_STALE, ofMonths(12)),
                    period(WORKFLOW, ofMonths(24)),
                    period(TODO, ofMonths(1)),
                    period(NOTIFICATION, ofMonths(1)),
                    period(ONBOARDING, ofMonths(6)),
                    period(AUDIT, ofMonths(12)),
                    period(USER_MASTER, ofYears(5)),
                    period(MANAGEMENT_ACCESS, ofMonths(12)));

    private BayernRetentionProfile() {}
}
