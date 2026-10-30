/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemGroupNamingService {

    private final TenantLocaleService tenantLocaleService;
    private final LocalizationService localizationService;

    public String resolveLocalizedName(OrganizationGroupType type, Long tenantId) {
        return resolveLocalizedName(type, tenantLocaleService.resolveTenantLocale(tenantId));
    }

    public String resolveLocalizedName(OrganizationGroupType type, SupportedLocale locale) {
        if (type == OrganizationGroupType.CUSTOM) {
            throw new IllegalArgumentException("Custom organization groups have no template name");
        }
        String localized = localizationService.getMessageOrNull(type.getMessageKey(), locale);
        if (localized != null) {
            return localized;
        }
        return type.name();
    }
}
