/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.list;

import com.emberone.management.domain.ManagementActivationStatus;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

/**
 * Optional filters of the activation log. Repeated params are combined with OR inside each
 * dimension; dimensions together use AND. Absent/empty lists mean "no restriction". {@code
 * organizationIds} matches an activation whose managing <em>or</em> target organization is listed.
 */
public record ManagementActivationListFilter(
        List<Long> tenantIds,
        List<ManagementActivationStatus> statuses,
        List<Long> organizationIds,
        List<Long> managingOrganizationIds) {

    public static final ManagementActivationListFilter NONE =
            new ManagementActivationListFilter(List.of(), List.of(), List.of(), List.of());

    public static ManagementActivationListFilter parse(
            List<Long> tenantId,
            List<String> status,
            List<Long> organizationId,
            List<Long> managingOrganizationId) {
        return new ManagementActivationListFilter(
                ListFilters.optionalLongIds(tenantId),
                ListFilters.parseEnumList(
                        status,
                        "status",
                        value -> {
                            try {
                                return Optional.of(ManagementActivationStatus.valueOf(value));
                            } catch (IllegalArgumentException e) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.optionalLongIds(organizationId),
                ListFilters.optionalLongIds(managingOrganizationId));
    }
}
