/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.module.domain.Module;
import com.emberone.organization.audience.OrganizationAudienceResolver;
import com.emberone.organization.domain.Organization;
import com.emberone.permission.domain.PermissionType;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The scope rules shared by activation and access: which target an audience entry covers, which
 * rights a target can carry, and which rights an entry allows. Activation checks them when the
 * activation is created, the access check re-evaluates them on every request.
 */
final class ManagementScope {

    private ManagementScope() {}

    static boolean covers(
            ManagementAudience entry, Organization target, List<Organization> activeOrganizations) {
        if (entry.getKind() == ManagementAudienceKind.TENANT) {
            return true;
        }
        return OrganizationAudienceResolver.resolve(
                        OrganizationAudienceResolver.Kind.valueOf(entry.getKind().name()),
                        entry.getRefId(),
                        activeOrganizations)
                .contains(target.getId());
    }

    /** The rights one entry allows: its permission types limited by the target's ceiling. */
    static Set<PermissionType> scopeOf(ManagementAudience entry, Set<PermissionType> ceiling) {
        return ManagementActivationService.decode(entry.getPermissionTypes()).stream()
                .filter(ceiling::contains)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PermissionType.class)));
    }

    /** Rights the target can carry: its enabled modules, without {@code MANAGEMENT_ACCESS}. */
    static Set<PermissionType> ceilingFor(
            Organization target,
            EffectiveStandardResolver effectiveStandardResolver,
            EnabledModuleResolver enabledModuleResolver) {
        Set<Module> modules =
                effectiveStandardResolver
                        .resolveForOrganization(target)
                        .map(enabledModuleResolver::forStandard)
                        .orElse(Set.of());
        return Arrays.stream(PermissionType.values())
                .filter(type -> type != PermissionType.MANAGEMENT_ACCESS)
                .filter(type -> modules.contains(type.getModule()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PermissionType.class)));
    }
}
