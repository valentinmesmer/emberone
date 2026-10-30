/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.WorkflowExclusivityLock;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowExclusivityLockRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowOpenExclusivityService {

    private final WorkflowExclusivityLockRepository lockRepository;

    @Transactional(readOnly = true)
    public void assertNoConflict(
            WorkflowHandler<?> handler, List<String> requestedKeys, Long excludeWorkflowId) {
        if (requestedKeys == null || requestedKeys.isEmpty()) {
            return;
        }
        for (String key : requestedKeys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (lockRepository.existsActiveConflict(key, excludeWorkflowId)) {
                throw new WorkflowValidationException(
                        handler.openExclusivityConflictMessage(),
                        handler.openExclusivityConflictCode());
            }
        }
    }

    @Transactional
    public void replaceLocks(
            WorkflowHandler<?> handler, Long tenantId, Long workflowId, List<String> keys) {
        lockRepository.deleteByWorkflowId(workflowId);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                unique.add(key);
            }
        }
        for (String key : unique) {
            try {
                lockRepository.save(
                        WorkflowExclusivityLock.builder()
                                .tenantId(tenantId)
                                .workflowId(workflowId)
                                .lockKey(key)
                                .build());
            } catch (DataIntegrityViolationException ex) {
                throw new WorkflowValidationException(
                        handler.openExclusivityConflictMessage(),
                        handler.openExclusivityConflictCode());
            }
        }
    }

    @Transactional
    public void releaseLocks(Long workflowId) {
        lockRepository.deleteByWorkflowId(workflowId);
    }
}
