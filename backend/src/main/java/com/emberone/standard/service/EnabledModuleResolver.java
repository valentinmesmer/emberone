/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.module.domain.Module;
import com.emberone.standard.cache.EnabledModulesCache;
import com.emberone.standard.domain.Standard;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves enabled {@link Module}s for a standard. Rows in {@code std_standard_modules} list
 * <em>disabled</em> toggleable modules; an empty table means all modules are on.
 */
@Service
@RequiredArgsConstructor
public class EnabledModuleResolver {

    private final EnabledModulesCache enabledModulesCache;

    public Set<Module> forStandard(Standard standard) {
        return enabledModulesCache.loadCached(standard.getTenantId(), standard.getId()).modules();
    }
}
