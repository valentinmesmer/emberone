/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate;

import com.emberone.federalstate.baseline.FederalStateBaselines;
import com.emberone.federalstate.baseline.TenantBaseline;
import com.emberone.federalstate.baseline.WorkflowDefinitionBaseline;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.rank.policy.RankPolicy;

/**
 * Federal-state-specific settings: baselines today; retention periods, age limits, labels later.
 */
public interface FederalStateSettings {

    FederalState federalState();

    FederalStateBaselines baselines();

    default TenantBaseline tenantBaseline() {
        return baselines().tenant();
    }

    default RankPolicy rankPolicy() {
        return RankPolicy.NO_OP;
    }

    default WorkflowDefinitionBaseline findWorkflowDefinition(Long workflowTypeId) {
        if (workflowTypeId == null) {
            return null;
        }
        return tenantBaseline().workflowDefinitions().stream()
                .filter(item -> workflowTypeId.equals(item.workflowTypeId()))
                .findFirst()
                .orElse(null);
    }
}
