/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

public record WorkflowTypeMappingBaseline(
        String configKey,
        long workflowTypeId,
        String orgGroupConfigKey,
        boolean autoApprove,
        boolean enabled,
        boolean selfApproveAllowed) {}
