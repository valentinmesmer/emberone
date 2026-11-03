/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.federalstate.baseline;

public record WorkflowTypeMappingBaseline(
        String configKey,
        long workflowTypeId,
        String orgGroupConfigKey,
        boolean autoApprove,
        boolean enabled,
        boolean selfApproveAllowed) {}
