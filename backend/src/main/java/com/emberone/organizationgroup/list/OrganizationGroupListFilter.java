/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
