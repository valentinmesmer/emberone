/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.management.list;

import com.emberone.admin.list.options.AdminOrganizationIdFilterOptionsProvider;
import com.emberone.admin.list.options.AdminTenantIdFilterOptionsProvider;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Filter options for {@code GET /api/v1/auth/admin/management-activations}. */
@Component
@RequiredArgsConstructor
public class AdminManagementActivationListFilterOptions implements ListFilterOptionsSource {

    private final TokenService tokenService;
    private final AdminTenantIdFilterOptionsProvider tenantIdOptions;
    private final AdminOrganizationIdFilterOptionsProvider organizationIdOptions;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        ListFilterOptionsContext context = ListFilterOptionsContext.admin();
        Map<String, List<ListFilterOptionDto>> options = new HashMap<>();
        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            options.put("tenantId", tenantIdOptions.resolve(context));
        }
        options.put("organizationId", organizationIdOptions.resolve(context));
        return new ListFilterOptionsDto(options);
    }
}
