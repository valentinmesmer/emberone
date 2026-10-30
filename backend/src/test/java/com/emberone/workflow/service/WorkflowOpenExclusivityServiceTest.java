/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.workflow.domain.WorkflowExclusivityLock;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowExclusivityLockRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenExclusivityKeys;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class WorkflowOpenExclusivityServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long WORKFLOW_ID = 50L;

    @Mock WorkflowExclusivityLockRepository lockRepository;

    private WorkflowOpenExclusivityService service;

    @BeforeEach
    void setUp() {
        service = new WorkflowOpenExclusivityService(lockRepository);
    }

    @Test
    void assertNoConflictAllowsWhenNoActiveLock() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        String key = WorkflowOpenExclusivityKeys.organizationMembershipExit(10L, 20L);
        when(lockRepository.existsActiveConflict(key, null)).thenReturn(false);

        assertThatCode(() -> service.assertNoConflict(handler, List.of(key), null))
                .doesNotThrowAnyException();
    }

    @Test
    void assertNoConflictRejectsWhenLockHeldByActiveWorkflow() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        when(handler.openExclusivityConflictMessage()).thenReturn("conflict");
        when(handler.openExclusivityConflictCode())
                .thenReturn(
                        WorkflowValidationException.CODE_ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN);
        String key = WorkflowOpenExclusivityKeys.organizationMembershipExit(10L, 20L);
        when(lockRepository.existsActiveConflict(key, null)).thenReturn(true);

        assertThatThrownBy(() -> service.assertNoConflict(handler, List.of(key), null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessage("conflict");
    }

    @Test
    void replaceLocksDeletesThenInsertsUniqueKeys() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        String key = WorkflowOpenExclusivityKeys.userMainOrganizationChange(15L);

        service.replaceLocks(handler, TENANT_ID, WORKFLOW_ID, List.of(key, key));

        verify(lockRepository).deleteByWorkflowId(WORKFLOW_ID);
        verify(lockRepository)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                lock ->
                                        lock.getTenantId().equals(TENANT_ID)
                                                && lock.getWorkflowId().equals(WORKFLOW_ID)
                                                && lock.getLockKey().equals(key)));
    }

    @Test
    void releaseLocksDeletesByWorkflow() {
        service.releaseLocks(WORKFLOW_ID);
        verify(lockRepository).deleteByWorkflowId(WORKFLOW_ID);
    }

    @Test
    void replaceLocksMapsUniqueConstraintViolationToValidationException() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        when(handler.openExclusivityConflictMessage()).thenReturn("locked");
        when(handler.openExclusivityConflictCode())
                .thenReturn(WorkflowValidationException.CODE_OPEN_EXCLUSIVITY_ALREADY_OPEN);
        String key = WorkflowOpenExclusivityKeys.userMainOrganizationChange(3L);
        when(lockRepository.save(org.mockito.ArgumentMatchers.any(WorkflowExclusivityLock.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(
                        () -> service.replaceLocks(handler, TENANT_ID, WORKFLOW_ID, List.of(key)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessage("locked");
    }
}
