/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.workflow.domain.*;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowGroupMembershipSyncServiceTest {

    @Mock WorkflowParticipantRepository participantRepository;
    @Mock WorkflowParticipantClaimService claimService;

    @InjectMocks WorkflowGroupMembershipSyncService service;

    @Test
    void releaseClaimsForUserLeavingGroupDelegatesToClaimService() {
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

        when(participantRepository.findOpenPendingClaimsByUserAndOrganizationGroup(42L, 30L))
                .thenReturn(List.of(participant));

        service.releaseClaimsForUserLeavingGroup(42L, 30L);

        verify(claimService).releasePendingClaim(participant, 42L);
    }

    @Test
    void releaseClaimsForUserLeavingGroupNoOpsWhenIdsMissing() {
        service.releaseClaimsForUserLeavingGroup(null, 30L);
        service.releaseClaimsForUserLeavingGroup(42L, null);

        verifyNoInteractions(participantRepository, claimService);
    }

    @Test
    void releaseClaimsForUserLeavingGroupNoOpsWhenNothingClaimed() {
        when(participantRepository.findOpenPendingClaimsByUserAndOrganizationGroup(42L, 30L))
                .thenReturn(List.of());

        service.releaseClaimsForUserLeavingGroup(42L, 30L);

        verify(claimService, never()).releasePendingClaim(any(), any());
    }
}
