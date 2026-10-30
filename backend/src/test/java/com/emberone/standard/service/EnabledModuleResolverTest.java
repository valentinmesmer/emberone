/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.module.domain.Module;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.cache.value.EnabledModulesCacheValue;
import com.emberone.standard.cache.EnabledModulesCache;
import com.emberone.standard.domain.Standard;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnabledModuleResolverTest {

    @Mock EnabledModulesCache enabledModulesCache;

    @InjectMocks EnabledModuleResolver resolver;

    private static Standard standard() {
        return Standard.builder().id(1L).tenantId(10L).build();
    }

    @Test
    void emptyTableMeansAllModulesEnabled() {
        when(enabledModulesCache.loadCached(10L, 1L))
                .thenReturn(new EnabledModulesCacheValue(EnumSet.allOf(Module.class)));

        assertThat(resolver.forStandard(standard())).containsExactlyInAnyOrder(Module.values());
    }

    @Test
    void storedRowMeansToggleableModuleDisabled() {
        when(enabledModulesCache.loadCached(10L, 1L))
                .thenReturn(
                        new EnabledModulesCacheValue(
                                EnumSet.of(Module.CORE, Module.OPERATIONS, Module.APPOINTMENTS)));

        Set<Module> enabled = resolver.forStandard(standard());
        assertThat(enabled).contains(Module.CORE, Module.APPOINTMENTS);
        assertThat(enabled).doesNotContain(Module.SERVICE);
    }

    @Test
    void allToggleableDisabledWhenAllFiveStored() {
        when(enabledModulesCache.loadCached(10L, 1L))
                .thenReturn(new EnabledModulesCacheValue(EnumSet.of(Module.CORE)));

        assertThat(resolver.forStandard(standard())).containsExactly(Module.CORE);
    }

    @Test
    void filterPermissionsRemovesGrantsForDisabledToggleableModules() {
        Set<Module> enabled = Module.alwaysOnModules();
        var filtered =
                Module.filterPermissions(
                        java.util.List.of(
                                PermissionType.USER_READ, PermissionType.SERVICE_EVENT_READ),
                        enabled);

        assertThat(filtered).containsExactly(PermissionType.USER_READ);
    }

    @Test
    void managementAccessIsStrippedWhenManagementModuleDisabled() {
        Set<Module> enabledWithoutManagement = Module.alwaysOnModules();
        var filtered =
                Module.filterPermissions(
                        java.util.List.of(
                                PermissionType.USER_READ, PermissionType.MANAGEMENT_ACCESS),
                        enabledWithoutManagement);

        assertThat(filtered).containsExactly(PermissionType.USER_READ);
    }
}
