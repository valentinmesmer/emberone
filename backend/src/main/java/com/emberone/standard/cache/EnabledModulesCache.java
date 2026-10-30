/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import com.emberone.module.domain.Module;
import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.EnabledModulesCacheValue;
import com.emberone.standard.domain.StandardModule;
import com.emberone.standard.repository.StandardModuleRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnabledModulesCache {

    private final StandardModuleRepository standardModuleRepository;

    @Cacheable(
            cacheNames = CacheNames.ENABLED_MODULES_BY_STANDARD,
            key = "#tenantId + ':' + #standardId")
    public EnabledModulesCacheValue loadCached(Long tenantId, Long standardId) {
        Set<Module> disabled =
                standardModuleRepository
                        .findAllByTenantIdAndStandardId(tenantId, standardId)
                        .stream()
                        .map(StandardModule::getModule)
                        .filter(Module::isToggleable)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Module.class)));
        Set<Module> enabled = Module.allEnabled();
        enabled.removeAll(disabled);
        return new EnabledModulesCacheValue(Set.copyOf(enabled));
    }
}
