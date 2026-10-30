/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.federalstate.baseline.FederalStateBaselines;
import com.emberone.federalstate.baseline.OrganizationSetupBaseline;
import com.emberone.federalstate.baseline.TenantBaseline;
import com.emberone.federalstate.bayern.BayernRetentionProfile;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.federalstate.exception.UnsupportedFederalStateException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FederalStateSettingsRegistryTest {

    private static FederalStateSettings stub(FederalState federalState) {
        return new FederalStateSettings() {
            private final FederalStateBaselines baselines =
                    new FederalStateBaselines(
                            new TenantBaseline(
                                    List.of(), List.of(), List.of(), List.of(), List.of(),
                                    List.of(), List.of(), List.of(), List.of(), List.of(),
                                    List.of()),
                            new OrganizationSetupBaseline(
                                    List.of(), List.of(), List.of(), List.of()),
                            BayernRetentionProfile.PROFILE);

            @Override
            public FederalState federalState() {
                return federalState;
            }

            @Override
            public FederalStateBaselines baselines() {
                return baselines;
            }
        };
    }

    @Test
    void configuredStates_listsRegisteredBeans() {
        FederalStateSettingsRegistry registry =
                new FederalStateSettingsRegistry(List.of(stub(FederalState.BAYERN)));

        assertThat(registry.configuredStates()).containsExactly(FederalState.BAYERN);
    }

    @Test
    void require_throwsForUnconfiguredState() {
        FederalStateSettingsRegistry registry =
                new FederalStateSettingsRegistry(List.of(stub(FederalState.BAYERN)));

        assertThatThrownBy(() -> registry.require(FederalState.NORDRHEIN_WESTFALEN))
                .isInstanceOf(UnsupportedFederalStateException.class);
    }
}
