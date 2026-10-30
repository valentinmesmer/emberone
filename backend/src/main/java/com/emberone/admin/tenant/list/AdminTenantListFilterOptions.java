/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.list;

import com.emberone.admin.list.options.AdminFederalStateFilterOptionsProvider;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Filter options for {@code GET /api/v1/auth/admin/tenants}. */
@Component
@RequiredArgsConstructor
public class AdminTenantListFilterOptions implements ListFilterOptionsSource {

    private final AdminFederalStateFilterOptionsProvider federalStateOptions;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        ListFilterOptionsContext context = ListFilterOptionsContext.admin();
        return new ListFilterOptionsDto(
                Map.of("federalState", federalStateOptions.resolve(context)));
    }
}
