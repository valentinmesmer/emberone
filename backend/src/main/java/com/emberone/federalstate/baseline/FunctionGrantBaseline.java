/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.federalstate.baseline;

import com.emberone.organizationgroup.domain.GroupMembershipType;

/** Maps an org-group template and membership role to a tenant function catalog key. */
public record FunctionGrantBaseline(
        String orgGroupConfigKey, GroupMembershipType membershipType, String functionConfigKey) {}
