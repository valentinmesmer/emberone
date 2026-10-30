/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.mockito.Mockito.*;

import com.emberone.user.event.UserDeactivatedEvent;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeactivatedWorkflowClaimCleanupListenerTest {

    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 7L;

    @Mock WorkflowParticipantRepository participantRepository;
    @Mock WorkflowParticipantClaimService claimService;

    @InjectMocks UserDeactivatedWorkflowClaimCleanupListener listener;

    @Test
    void onUserDeactivatedReleasesAllOpenClaimsForUser() {
        Workflow workflow = Workflow.builder().id(100L).build();
        WorkflowParticipant participant =
                WorkflowParticipant.builder()
                        .id(5L)
                        .workflow(workflow)
                        .claimedByUserId(USER_ID)
                        .build();
        when(participantRepository.findOpenPendingClaimsByUser(USER_ID))
                .thenReturn(List.of(participant));

        listener.onUserDeactivated(new UserDeactivatedEvent(TENANT_ID, USER_ID));

        verify(claimService).releasePendingClaim(participant, USER_ID);
    }
}
