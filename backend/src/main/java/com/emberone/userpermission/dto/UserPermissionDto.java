/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.dto;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.permission.domain.PermissionType;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionDto {
    Map<Long, List<PermissionType>> permissions;

    /** UI language of the user's tenant. */
    SupportedLocale tenantLocale;
}
