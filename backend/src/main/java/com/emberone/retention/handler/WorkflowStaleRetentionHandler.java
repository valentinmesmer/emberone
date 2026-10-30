/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.workflow.service.WorkflowService;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowStaleRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES = Set.of("wf_instances");

    private final WorkflowService workflowService;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.WORKFLOW_STALE;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        int cancelled = 0;
        for (Long workflowId : workflowService.findStaleWorkflowIds(cutoff)) {
            try {
                if (workflowService.cancelStaleWorkflow(workflowId, cutoff)) {
                    cancelled++;
                }
            } catch (RuntimeException e) {
                log.error("Cancelling stale workflow {} failed", workflowId, e);
            }
        }
        return cancelled;
    }
}
