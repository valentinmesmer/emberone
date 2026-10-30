/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention;

import static com.emberone.retention.profile.RetentionPeriod.ofDays;
import static com.emberone.retention.profile.RetentionPeriod.ofMonths;
import static com.emberone.retention.profile.RetentionPeriod.ofYears;
import static com.emberone.retention.profile.RetentionProfile.period;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.profile.RetentionProfile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class TestRetentionProfileFactory {

    private TestRetentionProfileFactory() {}

    public static RetentionProfile minimalValidProfile() {
        List<RetentionProfile.CategoryPeriod> entries = new ArrayList<>();
        for (RetentionCategory category : EnumSet.allOf(RetentionCategory.class)) {
            if (category == RetentionCategory.USER_MASTER) {
                entries.add(period(category, ofYears(5)));
            } else if (category == RetentionCategory.AUDIT) {
                entries.add(period(category, ofMonths(12)));
            } else if (category == RetentionCategory.DRIVING_LICENSE) {
                entries.add(period(category, ofYears(3)));
            } else if (category == RetentionCategory.EVIDENCE) {
                entries.add(period(category, ofMonths(12)));
            } else {
                entries.add(period(category, ofDays(28)));
            }
        }
        return RetentionProfile.of("TEST", entries.toArray(RetentionProfile.CategoryPeriod[]::new));
    }
}
