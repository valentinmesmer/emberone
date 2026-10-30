/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate;

import com.emberone.federalstate.domain.FederalState;
import com.emberone.federalstate.exception.UnsupportedFederalStateException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FederalStateSettingsRegistry {

    private final Map<FederalState, FederalStateSettings> settingsByState;

    public FederalStateSettingsRegistry(List<FederalStateSettings> settings) {
        Map<FederalState, FederalStateSettings> map = new EnumMap<>(FederalState.class);
        for (FederalStateSettings item : settings) {
            map.put(item.federalState(), item);
        }
        this.settingsByState = Map.copyOf(map);
    }

    public List<FederalState> configuredStates() {
        return settingsByState.keySet().stream().sorted().toList();
    }

    public Optional<FederalStateSettings> find(FederalState federalState) {
        return Optional.ofNullable(settingsByState.get(federalState));
    }

    public FederalStateSettings require(FederalState federalState) {
        return find(federalState)
                .orElseThrow(() -> new UnsupportedFederalStateException(federalState));
    }
}
