/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.list;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.federalstate.domain.FederalState;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

public final class TenantListSpecs {

    private TenantListSpecs() {}

    public static Specification<Tenant> inFederalStates(Collection<FederalState> federalStates) {
        return (root, query, cb) -> root.get("federalState").in(federalStates);
    }

    /** Applies the optional filters of {@link TenantListFilter}. */
    public static Specification<Tenant> matching(TenantListFilter filter) {
        Specification<Tenant> spec = (root, query, cb) -> cb.conjunction();
        if (!filter.federalStates().isEmpty()) {
            spec = spec.and(inFederalStates(filter.federalStates()));
        }
        return spec;
    }
}
