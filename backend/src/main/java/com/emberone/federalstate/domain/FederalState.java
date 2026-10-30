/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * German federal state (Bundesland). Immutable after tenant creation. Enum constant names are
 * German; JSON uses the same names (e.g. {@code "BAYERN"}).
 */
@Getter
@RequiredArgsConstructor
public enum FederalState {
    BADEN_WUERTTEMBERG,
    BAYERN,
    BERLIN,
    BRANDENBURG,
    BREMEN,
    HAMBURG,
    HESSEN,
    MECKLENBURG_VORPOMMERN,
    NIEDERSACHSEN,
    NORDRHEIN_WESTFALEN,
    RHEINLAND_PFALZ,
    SAARLAND,
    SACHSEN,
    SACHSEN_ANHALT,
    SCHLESWIG_HOLSTEIN,
    THUERINGEN;

    @JsonCreator
    public static FederalState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return BAYERN;
        }
        String normalized = value.trim().toUpperCase();
        for (FederalState state : values()) {
            if (state.name().equals(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported federal state: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
