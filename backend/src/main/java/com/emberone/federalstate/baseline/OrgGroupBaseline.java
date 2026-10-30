/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import com.emberone.organizationgroup.domain.OrganizationGroupType;

public record OrgGroupBaseline(
        String configKey, String name, String description, OrganizationGroupType groupType) {}
