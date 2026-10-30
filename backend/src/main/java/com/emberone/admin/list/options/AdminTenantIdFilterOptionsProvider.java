/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.list.options;

import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsAuthorization;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import com.emberone.platform.web.list.options.ListFilterOptionsProvider;
import com.emberone.platform.web.list.options.ListFilterOptionsScope;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminTenantIdFilterOptionsProvider implements ListFilterOptionsProvider {

    private final TenantRepository tenantRepository;
    private final ListFilterOptionsAuthorization listFilterOptionsAuthorization;

    @Override
    public String key() {
        return "tenantId";
    }

    @Override
    public Set<ListFilterOptionsScope> scopes() {
        return Set.of(ListFilterOptionsScope.ADMIN);
    }

    @Override
    public List<ListFilterOptionDto> resolve(ListFilterOptionsContext context) {
        if (!listFilterOptionsAuthorization.mayExposeTenantForAdminUserListFilter()) {
            return List.of();
        }
        return tenantRepository.findAll(Sort.by("name").ascending()).stream()
                .map(
                        tenant ->
                                new ListFilterOptionDto(
                                        String.valueOf(tenant.getId()), tenant.getName()))
                .toList();
    }
}
