/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.tenant.mapper;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.dto.TenantDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantDto toDto(Tenant tenant);
}
