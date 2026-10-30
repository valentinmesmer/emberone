/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.list;

import com.emberone.certification.dto.CertificationOverviewStatusFilter;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;

/**
 * Certification status overview filters. Repeated {@code status} and {@code type} (tenant cert
 * definition id) params are OR-combined within each dimension; empty means no restriction.
 */
public record CertificationOverviewListFilter(
        List<CertificationOverviewStatusFilter> statuses, List<Long> tenantCertDefinitionIds) {

    public CertificationOverviewListFilter {
        if (statuses == null) {
            statuses = List.of();
        }
        if (tenantCertDefinitionIds == null) {
            tenantCertDefinitionIds = List.of();
        }
    }

    public static CertificationOverviewListFilter parse(List<String> status, List<String> type) {
        return new CertificationOverviewListFilter(
                CertificationOverviewStatusFilter.parseStatuses(status),
                ListFilters.parseLongList(type, "type"));
    }
}
