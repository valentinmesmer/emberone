/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.list;

import com.emberone.federalstate.domain.FederalState;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

/**
 * Optional filters of the admin tenant directory. Repeated {@code federalState} query params are
 * combined with OR; absent/empty lists mean "no restriction".
 */
public record TenantListFilter(List<FederalState> federalStates) {

    public static final TenantListFilter NONE = new TenantListFilter(List.of());

    /** Parses request parameters. */
    public static TenantListFilter parse(List<String> federalState) {
        return new TenantListFilter(
                ListFilters.parseEnumList(
                        federalState, "federalState", TenantListFilter::parseFederalState));
    }

    private static Optional<FederalState> parseFederalState(String value) {
        try {
            return Optional.of(FederalState.fromValue(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
