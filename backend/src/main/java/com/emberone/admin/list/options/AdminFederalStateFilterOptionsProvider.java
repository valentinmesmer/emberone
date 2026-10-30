/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.list.options;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.federalstate.FederalStateSettingsRegistry;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import com.emberone.platform.web.list.options.ListFilterOptionsProvider;
import com.emberone.platform.web.list.options.ListFilterOptionsScope;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminFederalStateFilterOptionsProvider implements ListFilterOptionsProvider {

    private final FederalStateSettingsRegistry federalStateSettingsRegistry;
    private final LocalizationService localizationService;
    private final TenantLocaleService tenantLocaleService;
    private final TokenService tokenService;

    @Override
    public String key() {
        return "federalState";
    }

    @Override
    public Set<ListFilterOptionsScope> scopes() {
        return Set.of(ListFilterOptionsScope.ADMIN);
    }

    @Override
    public List<ListFilterOptionDto> resolve(ListFilterOptionsContext context) {
        SupportedLocale locale =
                tenantLocaleService.resolveTenantLocale(tokenService.getTenantId());
        return federalStateSettingsRegistry.configuredStates().stream()
                .map(
                        state ->
                                new ListFilterOptionDto(
                                        state.name(),
                                        localizationService.getMessage(
                                                "federalState." + state.name(), locale)))
                .toList();
    }
}
