/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.workflow.domain.*;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowEventPublisher;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowParticipantClaimServiceTest {

    @Mock WorkflowParticipantRepository participantRepository;
    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowEventPublisher eventPublisher;

    @InjectMocks WorkflowParticipantClaimService service;

    @Test
    void releasePendingClaimClearsClaimAndReopensGroupTodo() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .tenantId(1L)
                        .openedByUserId(2L)
                        .status(WorkflowStatus.OPEN)
                        .participants(new LinkedHashSet<>())
                        .build();
        WorkflowParticipant participant =
                WorkflowParticipant.builder()
                        .id(10L)
                        .workflow(workflow)
                        .role(ParticipantRole.APPROVER)
                        .principalType(PrincipalType.ORGANIZATION_GROUP)
                        .principalId(30L)
                        .slotStatus(ParticipantSlotStatus.PENDING)
                        .claimedByUserId(42L)
                        .build();
        workflow.setParticipants(new LinkedHashSet<>(Set.of(participant)));

        when(workflowRepository.findWithDetailsById(5L)).thenReturn(Optional.of(workflow));

        service.releasePendingClaim(participant, 42L);

        verify(participantRepository).save(participant);
        verify(eventPublisher)
                .publish(
                        workflow,
                        WorkflowLifecycleEvent.UNCLAIMED,
                        WorkflowEventContext.unclaimed(10L, 42L));
    }
}
