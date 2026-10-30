/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowHandlerRegistry {

    private final List<WorkflowHandler<?>> handlers;
    private final Map<Long, WorkflowHandler<?>> handlerByTypeId = new HashMap<>();

    @PostConstruct
    void register() {
        for (WorkflowHandler<?> handler : handlers) {
            WorkflowHandler<?> previous = handlerByTypeId.put(handler.workflowTypeId(), handler);
            if (previous != null) {
                throw new WorkflowConfigurationException(
                        "Duplicate workflow handler registered for type id "
                                + handler.workflowTypeId());
            }
        }
        log.debug(
                "Registered {} workflow handler(s): {}",
                handlerByTypeId.size(),
                handlerByTypeId.keySet());
    }

    public WorkflowHandler<?> require(Long workflowTypeId) {
        if (workflowTypeId == null) {
            throw new WorkflowConfigurationException("workflowTypeId is required");
        }
        if (!WorkflowType.isKnownId(workflowTypeId)) {
            throw new ResourceNotFoundException("Unknown workflow type id: " + workflowTypeId);
        }
        WorkflowHandler<?> handler = handlerByTypeId.get(workflowTypeId);
        if (handler == null) {
            throw new WorkflowConfigurationException(
                    "No workflow handler registered for type id " + workflowTypeId);
        }
        return handler;
    }

    public List<WorkflowHandler<?>> all() {
        return List.copyOf(handlerByTypeId.values());
    }
}
