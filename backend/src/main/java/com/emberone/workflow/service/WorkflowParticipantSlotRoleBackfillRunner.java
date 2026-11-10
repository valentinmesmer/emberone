/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Temporary rollout runner for the approver slot roles ({@code V3}); remove it after the rollout
 * once every tenant has started once with it.
 *
 * <p>The migration gives every participant the role {@code CONTEXT}. At startup this runner sets
 * the real role (for example {@code NEW_MAIN_ORG} or {@code ADDITIONAL}) on the org approver slots
 * of open instances that still have the migration default. It only looks at instances with several
 * org approver slots and no role other than {@code CONTEXT} yet, so after the rollout a startup is
 * one cheap query per tenant that finds nothing. Each instance runs in its own transaction; an
 * instance that cannot be resolved is logged and skipped. It changes no decisions or claims. Limit:
 * an instance with a single approver slot that is an extra org (exclusive routing) keeps {@code
 * CONTEXT}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowParticipantSlotRoleBackfillRunner {

    private final TenantBatchRunner tenantBatchRunner;
    private final WorkflowService workflowService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        tenantBatchRunner.forEachTenant(tenantId -> {
            int updated = 0;
            for (Long workflowId : workflowService.findWorkflowIdsAwaitingSlotRoleBackfill()) {
                try {
                    updated += workflowService.backfillApproverSlotRoles(workflowId);
                } catch (Exception ex) {
                    log.warn(
                            "Skipping slot role backfill for workflow {} of tenant {}: {}",
                            workflowId,
                            tenantId,
                            ex.getMessage());
                }
            }
            if (updated > 0) {
                log.info(
                        "Backfilled the slot role of {} approver slot(s) for tenant {}",
                        updated,
                        tenantId);
            }
        });
    }
}
