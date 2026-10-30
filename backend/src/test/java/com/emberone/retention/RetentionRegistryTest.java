/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention;

import static com.emberone.retention.domain.RetentionCategory.AUDIT;
import static com.emberone.retention.domain.RetentionCategory.USER_MASTER;
import static com.emberone.retention.profile.RetentionPeriod.ofDays;
import static com.emberone.retention.profile.RetentionPeriod.ofMonths;
import static com.emberone.retention.profile.RetentionPeriod.ofYears;
import static com.emberone.retention.profile.RetentionProfile.period;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.federalstate.bayern.BayernRetentionProfile;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.exception.RetentionConfigurationException;
import com.emberone.retention.handler.NoOpRetentionHandler;
import com.emberone.retention.profile.RetentionProfile;
import com.emberone.retention.spi.RetentionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RetentionRegistryTest {

    @Test
    void constructor_failsWhenHandlerMissing() {
        List<RetentionHandler> handlers = new ArrayList<>();
        for (RetentionCategory category : RetentionCategory.values()) {
            if (category == RetentionCategory.EXIT_IMMEDIATE) {
                continue;
            }
            handlers.add(new NoOpRetentionHandler(category, Set.of("users")));
        }
        assertThatThrownBy(() -> new RetentionRegistry(handlers))
                .isInstanceOf(RetentionConfigurationException.class)
                .hasMessageContaining("EXIT_IMMEDIATE");
    }

    @Test
    void profile_failsWhenCategoryMissing() {
        assertThatThrownBy(
                        () ->
                                RetentionProfile.of(
                                        "INCOMPLETE",
                                        period(AUDIT, ofMonths(12)),
                                        period(USER_MASTER, ofYears(5))))
                .isInstanceOf(RetentionConfigurationException.class);
    }

    @Test
    void profile_failsWhenInvariantViolated() {
        assertThatThrownBy(() -> profileWithShortUserMaster())
                .isInstanceOf(RetentionConfigurationException.class)
                .hasMessageContaining("AUDIT");
    }

    private static RetentionProfile profileWithShortUserMaster() {
        List<RetentionProfile.CategoryPeriod> entries = new ArrayList<>();
        for (RetentionCategory category : RetentionCategory.values()) {
            if (category == USER_MASTER) {
                entries.add(period(category, ofYears(4)));
            } else if (category == AUDIT) {
                entries.add(period(category, ofYears(5)));
            } else if (category == RetentionCategory.DRIVING_LICENSE) {
                entries.add(period(category, ofYears(3)));
            } else if (category == RetentionCategory.EVIDENCE) {
                entries.add(period(category, ofMonths(12)));
            } else {
                entries.add(period(category, ofDays(28)));
            }
        }
        return RetentionProfile.of("BAD", entries.toArray(RetentionProfile.CategoryPeriod[]::new));
    }

    private static List<RetentionHandler> allNoOpHandlers() {
        List<RetentionHandler> handlers = new ArrayList<>();
        for (RetentionCategory category : RetentionCategory.values()) {
            handlers.add(new NoOpRetentionHandler(category, Set.of("users")));
        }
        return handlers;
    }

    @Test
    void constructor_acceptsCompleteConfiguration() {
        new RetentionRegistry(allNoOpHandlers());
    }

    @Test
    void bayernProfile_isValid() {
        assertThat(BayernRetentionProfile.PROFILE.key()).isEqualTo(BayernRetentionProfile.KEY);
        assertThat(BayernRetentionProfile.KEY).isEqualTo("BY");
    }
}
