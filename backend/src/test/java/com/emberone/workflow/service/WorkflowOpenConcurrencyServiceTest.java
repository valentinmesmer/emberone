/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowOpenConcurrencyServiceTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 7L;

    @Mock WorkflowRepository workflowRepository;

    private WorkflowOpenConcurrencyService service;

    @BeforeEach
    void setUp() {
        service =
                new WorkflowOpenConcurrencyService(
                        workflowRepository, JsonMapper.builder().build());
    }

    @Test
    void assertNoConflictSkipsRepositoryWhenRequestedKeysAreEmpty() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);

        service.assertNoConflict(handler, USER_ID, List.of(), null);
        service.assertNoConflict(handler, USER_ID, null, null);

        verifyNoInteractions(workflowRepository);
    }

    @Test
    void assertNoConflictAllowsNonOverlappingKeys() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        when(handler.workflowTypeId()).thenReturn(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId());
        when(workflowRepository.findActiveByOpenedByUserIdAndWorkflowTypeId(
                        USER_ID, WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(
                        List.of(
                                workflowWithKeys(
                                        50L,
                                        List.of(WorkflowOpenConcurrencyKeys.organization(99L)))));

        assertThatCode(
                        () ->
                                service.assertNoConflict(
                                        handler,
                                        USER_ID,
                                        List.of(WorkflowOpenConcurrencyKeys.organization(20L)),
                                        null))
                .doesNotThrowAnyException();
    }

    @Test
    void assertNoConflictIgnoresExcludedWorkflow() {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        when(handler.workflowTypeId()).thenReturn(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId());
        when(workflowRepository.findActiveByOpenedByUserIdAndWorkflowTypeId(
                        USER_ID, WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(
                        List.of(
                                workflowWithKeys(
                                        50L,
                                        List.of(WorkflowOpenConcurrencyKeys.organization(20L)))));

        assertThatCode(
                        () ->
                                service.assertNoConflict(
                                        handler,
                                        USER_ID,
                                        List.of(WorkflowOpenConcurrencyKeys.organization(20L)),
                                        50L))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("handlerConflictCases")
    void assertNoConflictThrowsHandlerSpecificErrorCode(
            WorkflowType workflowType, String conflictCode, String requestedKey) {
        WorkflowHandler<?> handler = mock(WorkflowHandler.class);
        when(handler.workflowTypeId()).thenReturn(workflowType.getId());
        when(handler.openConcurrencyConflictCode()).thenReturn(conflictCode);
        when(handler.openConcurrencyConflictMessage()).thenReturn("Conflict");
        when(workflowRepository.findActiveByOpenedByUserIdAndWorkflowTypeId(
                        eq(USER_ID), eq(workflowType.getId())))
                .thenReturn(List.of(workflowWithKeys(99L, List.of(requestedKey))));

        assertThatThrownBy(
                        () ->
                                service.assertNoConflict(
                                        handler, USER_ID, List.of(requestedKey), null))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting(ex -> ((WorkflowValidationException) ex).getCode())
                .isEqualTo(conflictCode);
    }

    @Test
    void findOpenKeysAggregatesKeysFromActiveWorkflows() {
        when(workflowRepository.findActiveByOpenedByUserIdAndWorkflowTypeId(
                        USER_ID, WorkflowType.CERTIFICATION_RECORD.getId()))
                .thenReturn(
                        List.of(
                                workflowWithKeys(
                                        10L,
                                        List.of(
                                                WorkflowOpenConcurrencyKeys.certificationDefinition(
                                                        100L))),
                                workflowWithKeys(
                                        11L,
                                        List.of(
                                                WorkflowOpenConcurrencyKeys.certificationDefinition(
                                                        200L)))));

        assertThat(service.findOpenKeys(USER_ID, WorkflowType.CERTIFICATION_RECORD.getId()))
                .containsExactlyInAnyOrder(
                        WorkflowOpenConcurrencyKeys.certificationDefinition(100L),
                        WorkflowOpenConcurrencyKeys.certificationDefinition(200L));
    }

    private static Stream<Arguments> handlerConflictCases() {
        return Stream.of(
                Arguments.of(
                        WorkflowType.ORGANIZATION_JOIN_REQUEST,
                        WorkflowValidationException.CODE_ORGANIZATION_JOIN_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.organization(20L)),
                Arguments.of(
                        WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST,
                        WorkflowValidationException.CODE_ORGANIZATION_GROUP_JOIN_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.organizationGroup(30L)),
                Arguments.of(
                        WorkflowType.ORGANIZATION_LEAVE_REQUEST,
                        WorkflowValidationException.CODE_ORGANIZATION_LEAVE_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.organization(20L)),
                Arguments.of(
                        WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST,
                        WorkflowValidationException.CODE_ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.organizationGroup(30L)),
                Arguments.of(
                        WorkflowType.MAIN_ORGANIZATION_CHANGE,
                        WorkflowValidationException.CODE_MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.organization(20L)),
                Arguments.of(
                        WorkflowType.CERTIFICATION_RECORD,
                        WorkflowValidationException.CODE_CERTIFICATION_DEFINITION_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.certificationDefinition(100L)),
                Arguments.of(
                        WorkflowType.TRAINING_COURSE_RECORD,
                        WorkflowValidationException.CODE_TRAINING_COURSE_TYPE_ALREADY_OPEN,
                        WorkflowOpenConcurrencyKeys.trainingCourseType(100L)));
    }

    private Workflow workflowWithKeys(long id, List<String> keys) {
        return Workflow.builder().id(id).openConcurrencyKeys(service.serializeKeys(keys)).build();
    }
}
