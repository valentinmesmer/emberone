/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.workflow.domain.*;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class WorkflowInputChangeRulesTest {

    private static Workflow baseWorkflow() {
        return Workflow.builder()
                .id(1L)
                .tenantId(1L)
                .workflowTypeId(1L)
                .status(WorkflowStatus.OPEN)
                .openedByUserId(10L)
                .participants(new LinkedHashSet<>())
                .decisions(new LinkedHashSet<>())
                .inputChanges(new LinkedHashSet<>())
                .build();
    }

    @Test
    void canEditInput_beforeTakenOver() {
        Workflow workflow = baseWorkflow();
        assertThat(WorkflowInputChangeRules.canEditInput(workflow, 10L)).isTrue();
        assertThat(WorkflowInputChangeRules.canEditInput(workflow, 11L)).isFalse();

        workflow.getParticipants()
                .add(
                        WorkflowParticipant.builder()
                                .id(1L)
                                .role(ParticipantRole.APPROVER)
                                .principalType(PrincipalType.USER)
                                .principalId(99L)
                                .claimedByUserId(99L)
                                .slotStatus(ParticipantSlotStatus.PENDING)
                                .build());
        assertThat(WorkflowInputChangeRules.canEditInput(workflow, 10L)).isTrue();

        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .id(1L)
                                .userId(7L)
                                .decisionType(DecisionType.APPROVED)
                                .build());
        assertThat(WorkflowInputChangeRules.canEditInput(workflow, 10L)).isFalse();
    }

    @Test
    void canEditInput_afterTakenOverOnlyWhenAwaiting() {
        Workflow workflow = baseWorkflow();
        workflow.getParticipants()
                .add(
                        WorkflowParticipant.builder()
                                .id(1L)
                                .role(ParticipantRole.APPROVER)
                                .principalType(PrincipalType.ORGANIZATION_GROUP)
                                .principalId(5L)
                                .claimedByUserId(42L)
                                .slotStatus(ParticipantSlotStatus.PENDING)
                                .build());

        assertThat(WorkflowInputChangeRules.canEditInput(workflow, 10L)).isFalse();

        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        assertThat(WorkflowInputChangeRules.canEditInput(workflow, 10L)).isTrue();
    }

    @Test
    void canRequestInputChange_requiresTakenOverAndClaimer() {
        Workflow workflow = baseWorkflow();
        workflow.getParticipants()
                .add(
                        WorkflowParticipant.builder()
                                .id(1L)
                                .role(ParticipantRole.APPROVER)
                                .principalType(PrincipalType.ORGANIZATION_GROUP)
                                .principalId(5L)
                                .claimedByUserId(42L)
                                .slotStatus(ParticipantSlotStatus.PENDING)
                                .build());

        assertThat(WorkflowInputChangeRules.canRequestInputChange(workflow, 42L)).isTrue();
        assertThat(WorkflowInputChangeRules.canRequestInputChange(workflow, 99L)).isFalse();

        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        assertThat(WorkflowInputChangeRules.canRequestInputChange(workflow, 42L)).isFalse();
    }

    @Test
    void canWithdrawInputChange_onlyRequesterWhileAwaiting() {
        Workflow workflow = baseWorkflow();
        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        workflow.setInputChanges(new LinkedHashSet<>());
        workflow.getInputChanges()
                .add(
                        WorkflowInputChange.builder()
                                .tenantId(1L)
                                .workflow(workflow)
                                .requestedByUserId(42L)
                                .message("Bitte korrigieren")
                                .status(WorkflowInputChangeStatus.OPEN)
                                .build());

        assertThat(WorkflowInputChangeRules.canWithdrawInputChange(workflow, 42L)).isTrue();
        assertThat(WorkflowInputChangeRules.canWithdrawInputChange(workflow, 10L)).isFalse();
        assertThat(WorkflowInputChangeRules.canWithdrawInputChange(workflow, 99L)).isFalse();

        workflow.setStatus(WorkflowStatus.OPEN);
        assertThat(WorkflowInputChangeRules.canWithdrawInputChange(workflow, 42L)).isFalse();
    }
}
