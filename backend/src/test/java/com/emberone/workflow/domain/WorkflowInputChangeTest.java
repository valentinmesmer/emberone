/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class WorkflowInputChangeTest {

    @Test
    void findOpenInputChange_returnsOnlyOpenRequest() {
        Workflow workflow =
                Workflow.builder()
                        .id(1L)
                        .tenantId(1L)
                        .workflowTypeId(1L)
                        .status(WorkflowStatus.OPEN)
                        .openedByUserId(10L)
                        .inputChanges(new LinkedHashSet<>())
                        .build();

        WorkflowInputChange fulfilled =
                WorkflowInputChange.builder()
                        .id(1L)
                        .tenantId(1L)
                        .workflow(workflow)
                        .requestedByUserId(20L)
                        .message("Bitte korrigieren")
                        .status(WorkflowInputChangeStatus.FULFILLED)
                        .fulfilledAt(Instant.now().minus(Duration.ofDays(1)))
                        .build();
        WorkflowInputChange open =
                WorkflowInputChange.builder()
                        .id(2L)
                        .tenantId(1L)
                        .workflow(workflow)
                        .requestedByUserId(20L)
                        .message("Noch einmal prüfen")
                        .status(WorkflowInputChangeStatus.OPEN)
                        .build();
        workflow.getInputChanges().add(fulfilled);
        workflow.getInputChanges().add(open);

        assertThat(workflow.findOpenInputChange()).contains(open);
    }

    @Test
    void fulfillAndCancelUpdateStatus() {
        WorkflowInputChange change =
                WorkflowInputChange.builder()
                        .status(WorkflowInputChangeStatus.OPEN)
                        .message("Bitte ergänzen")
                        .build();
        Instant at = LocalDateTime.of(2026, 8, 2, 12, 0).toInstant(ZoneOffset.UTC);

        change.fulfill(at);
        assertThat(change.getStatus()).isEqualTo(WorkflowInputChangeStatus.FULFILLED);
        assertThat(change.getFulfilledAt()).isEqualTo(at);
        assertThat(change.isOpen()).isFalse();

        WorkflowInputChange open =
                WorkflowInputChange.builder()
                        .status(WorkflowInputChangeStatus.OPEN)
                        .message("Bitte ergänzen")
                        .build();
        open.cancel(at);
        assertThat(open.getStatus()).isEqualTo(WorkflowInputChangeStatus.CANCELLED);
        assertThat(open.getFulfilledAt()).isEqualTo(at);
    }
}
