/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.profile;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.exception.RetentionConfigurationException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Named retention profile: one period per {@link RetentionCategory}. */
public final class RetentionProfile {

    private final String key;
    private final Map<RetentionCategory, RetentionPeriod> periods;

    private RetentionProfile(String key, Map<RetentionCategory, RetentionPeriod> periods) {
        this.key = Objects.requireNonNull(key, "key");
        this.periods = Map.copyOf(periods);
    }

    public static RetentionProfile of(String key, CategoryPeriod... entries) {
        Objects.requireNonNull(entries, "entries");
        Map<RetentionCategory, RetentionPeriod> mapped = new EnumMap<>(RetentionCategory.class);
        for (CategoryPeriod entry : entries) {
            Objects.requireNonNull(entry, "entry");
            RetentionPeriod previous = mapped.put(entry.category(), entry.period());
            if (previous != null) {
                throw new RetentionConfigurationException(
                        "Duplicate retention period for "
                                + entry.category()
                                + " in profile "
                                + key);
            }
        }
        for (RetentionCategory category : RetentionCategory.values()) {
            if (!mapped.containsKey(category)) {
                throw new RetentionConfigurationException(
                        "Missing retention period for " + category + " in profile " + key);
            }
        }
        RetentionProfile profile = new RetentionProfile(key, mapped);
        profile.validateInvariants();
        return profile;
    }

    public static CategoryPeriod period(RetentionCategory category, RetentionPeriod period) {
        return new CategoryPeriod(category, period);
    }

    public String key() {
        return key;
    }

    public RetentionPeriod period(RetentionCategory category) {
        RetentionPeriod period = periods.get(category);
        if (period == null) {
            throw new RetentionConfigurationException(
                    "No retention period for " + category + " in profile " + key);
        }
        return period;
    }

    private void validateInvariants() {
        RetentionPeriod userMaster = period(RetentionCategory.USER_MASTER);
        RetentionPeriod drivingLicense = period(RetentionCategory.DRIVING_LICENSE);
        RetentionPeriod audit = period(RetentionCategory.AUDIT);

        if (userMaster.isStrictlyShorterThan(drivingLicense)) {
            throw new RetentionConfigurationException(
                    "Profile " + key + " violates invariant USER_MASTER >= DRIVING_LICENSE");
        }
        if (!audit.isStrictlyShorterThan(userMaster)) {
            throw new RetentionConfigurationException(
                    "Profile " + key + " violates invariant AUDIT < USER_MASTER");
        }
    }

    public record CategoryPeriod(RetentionCategory category, RetentionPeriod period) {}
}
