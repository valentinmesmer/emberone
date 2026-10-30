/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Reconciles open workflow instances after admin configuration changes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowConfigurationSyncService {

    private final WorkflowService workflowService;

    @AfterCommitEventListener
    public void onWorkflowConfigurationChanged(WorkflowConfigurationChangedEvent event) {
        if (event == null
                || event.tenantId() == null
                || event.workflowTypeId() == null
                || event.scope() == null) {
            return;
        }
        log.debug(
                "Reconciling open workflows for tenant {} type {} scope {} orgs {}",
                event.tenantId(),
                event.workflowTypeId(),
                event.scope(),
                event.organizationIds());
        workflowService.reconcileOpenInstancesAfterConfigurationChange(event);
    }
}
