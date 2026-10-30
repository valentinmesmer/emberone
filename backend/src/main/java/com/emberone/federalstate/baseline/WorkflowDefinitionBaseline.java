/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

public record WorkflowDefinitionBaseline(
        String configKey, long workflowTypeId, boolean staticApproversExclusive) {}
