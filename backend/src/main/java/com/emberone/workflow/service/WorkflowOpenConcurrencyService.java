/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class WorkflowOpenConcurrencyService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final WorkflowRepository workflowRepository;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public Set<String> findOpenKeys(Long userId, long workflowTypeId) {
        return findOpenKeys(userId, workflowTypeId, null);
    }

    @Transactional(readOnly = true)
    public Set<String> findOpenKeys(Long userId, long workflowTypeId, Long excludeWorkflowId) {
        return workflowRepository
                .findActiveByOpenedByUserIdAndWorkflowTypeId(userId, workflowTypeId)
                .stream()
                .filter(
                        workflow ->
                                excludeWorkflowId == null
                                        || !excludeWorkflowId.equals(workflow.getId()))
                .flatMap(workflow -> deserializeKeys(workflow.getOpenConcurrencyKeys()).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void assertNoConflict(
            WorkflowHandler<?> handler,
            Long userId,
            List<String> requestedKeys,
            Long excludeWorkflowId) {
        if (requestedKeys == null || requestedKeys.isEmpty()) {
            return;
        }
        Set<String> openKeys = findOpenKeys(userId, handler.workflowTypeId(), excludeWorkflowId);
        for (String requestedKey : requestedKeys) {
            if (openKeys.contains(requestedKey)) {
                throw new WorkflowValidationException(
                        handler.openConcurrencyConflictMessage(),
                        handler.openConcurrencyConflictCode());
            }
        }
    }

    public String serializeKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(keys);
        } catch (JacksonException e) {
            throw new WorkflowValidationException("Failed to serialize workflow concurrency keys");
        }
    }

    public List<String> deserializeKeys(String storedKeys) {
        if (storedKeys == null || storedKeys.isBlank()) {
            return List.of();
        }
        try {
            List<String> keys = jsonMapper.readValue(storedKeys, STRING_LIST);
            if (keys == null || keys.isEmpty()) {
                return List.of();
            }
            return List.copyOf(keys);
        } catch (JacksonException e) {
            return List.of();
        }
    }
}
