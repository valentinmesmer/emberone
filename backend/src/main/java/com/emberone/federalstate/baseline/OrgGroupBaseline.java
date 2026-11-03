/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.federalstate.baseline;

import com.emberone.organizationgroup.domain.OrganizationGroupType;

public record OrgGroupBaseline(
        String configKey, String name, String description, OrganizationGroupType groupType) {}
