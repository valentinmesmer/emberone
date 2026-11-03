/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organizationgroup.list;

import org.springframework.web.bind.annotation.RequestParam;

public record OrganizationGroupListFilter(
        @RequestParam(required = false) OrganizationGroupListStatus status) {

    public OrganizationGroupListFilter {
        if (status == null) {
            status = OrganizationGroupListStatus.ACTIVE;
        }
    }
}
