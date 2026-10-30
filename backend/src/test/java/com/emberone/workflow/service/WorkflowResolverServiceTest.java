/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.workflow.domain.*;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.spi.ParticipantSpec;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowResolverServiceTest {

    @Mock private WorkflowTypeMappingService mappingService;

    @InjectMocks private WorkflowResolverService service;

    @Test
    void syncOrgApproverSlots_addsMissingOrgSlotAndRemovesObsoletePendingSlot() {
        Workflow workflow = workflowWithParticipants();
        WorkflowParticipant obsolete =
                WorkflowParticipant.builder()
                        .tenantId(1L)
                        .workflow(workflow)
                        .role(ParticipantRole.APPROVER)
                        .requirement(ParticipantRequirement.MANDATORY)
                        .principalType(PrincipalType.ORGANIZATION_GROUP)
                        .principalId(100L)
                        .organizationId(9L)
                        .slotKey("AGR:9")
                        .slotStatus(ParticipantSlotStatus.PENDING)
                        .build();
        workflow.getParticipants().add(obsolete);

        when(mappingService.requireMapping(8L, 2L))
                .thenReturn(
                        WorkflowTypeMapping.builder()
                                .organizationGroupId(200L)
                                .autoApprove(false)
                                .build());

        WorkflowResolverService.OrgApproverSlotSyncResult result =
                service.syncOrgApproverSlots(workflow, 2L, 5L, List.of(8L), true, List.of());

        assertThat(result.changed()).isTrue();
        assertThat(result.removedPendingParticipants()).containsExactly(obsolete);
        assertThat(workflow.getParticipants())
                .extracting(WorkflowParticipant::getSlotKey)
                .containsExactly("AGR:8");
    }

    @Test
    void resolveApproverOrganizationIds_collapsesIdenticalContextAndDynamicOrganization() {
        // Course registration: offering organization (context) and the user's main organization
        // (dynamic) are the same organization, so a single decision is enough.
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L, List.of(), false, List.of(5L)))
                .containsExactly(5L);
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L, List.of(), false, List.of(6L)))
                .containsExactly(5L, 6L);
    }

    @Test
    void resolveParticipants_createsApproverForContextOrganization() {
        Workflow workflow = workflowWithParticipants();
        WorkflowTypeMapping mapping =
                WorkflowTypeMapping.builder().organizationGroupId(100L).autoApprove(false).build();
        when(mappingService.requireMapping(5L, 1L)).thenReturn(mapping);

        service.resolveParticipants(workflow, 99L, 1L, 5L, List.of(), false, false, null, null);

        assertThat(workflow.getParticipants()).hasSize(1);
        WorkflowParticipant participant = workflow.getParticipants().iterator().next();
        assertThat(participant.getSlotKey()).isEqualTo("AGR:5");
        assertThat(participant.getPrincipalId()).isEqualTo(100L);
        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.PENDING);
    }

    @Test
    void resolveParticipants_staticApproversExclusiveUsesOnlyStaticOrganizations() {
        Workflow workflow = workflowWithParticipants();
        WorkflowTypeMapping mapping =
                WorkflowTypeMapping.builder().organizationGroupId(200L).autoApprove(true).build();
        when(mappingService.requireMapping(8L, 2L)).thenReturn(mapping);

        service.resolveParticipants(
                workflow, 1L, 2L, 5L, List.of(8L), true, false, List.of(9L), null);

        assertThat(workflow.getParticipants()).hasSize(1);
        WorkflowParticipant participant = workflow.getParticipants().iterator().next();
        assertThat(participant.getSlotKey()).isEqualTo("AGR:8");
        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.APPROVED);
    }

    @Test
    void resolveParticipants_autoClaimsUserApproverWhenOpenedByDifferentUser() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireMapping(5L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(100L).build());

        ParticipantSpec spec =
                ParticipantSpec.builder()
                        .role(ParticipantRole.APPROVER)
                        .principalType(PrincipalType.USER)
                        .principalId(77L)
                        .build();

        service.resolveParticipants(
                workflow, 99L, 1L, 5L, List.of(), false, false, null, List.of(spec));

        WorkflowParticipant userApprover =
                workflow.getParticipants().stream()
                        .filter(p -> p.getPrincipalType() == PrincipalType.USER)
                        .findFirst()
                        .orElseThrow();
        assertThat(userApprover.getClaimedByUserId()).isEqualTo(77L);
        assertThat(userApprover.getClaimedAt()).isNotNull();
    }

    @Test
    void resolveParticipants_includesDynamicOrganizationsWhenNotExclusive() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireMapping(5L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(100L).build());
        when(mappingService.requireMapping(9L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(200L).build());

        service.resolveParticipants(
                workflow, 1L, 1L, 5L, List.of(), false, false, List.of(9L), null);

        assertThat(workflow.getParticipants())
                .extracting(WorkflowParticipant::getSlotKey)
                .containsExactlyInAnyOrder("AGR:5", "AGR:9");
    }

    @Test
    void resolveParticipants_autoClaimsOpenerWhenSelfApproveAllowed() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireMapping(5L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(100L).build());

        ParticipantSpec spec =
                ParticipantSpec.builder()
                        .role(ParticipantRole.APPROVER)
                        .principalType(PrincipalType.USER)
                        .principalId(42L)
                        .build();

        service.resolveParticipants(
                workflow, 42L, 1L, 5L, List.of(), false, true, null, List.of(spec));

        WorkflowParticipant userApprover =
                workflow.getParticipants().stream()
                        .filter(p -> p.getPrincipalType() == PrincipalType.USER)
                        .findFirst()
                        .orElseThrow();
        assertThat(userApprover.getClaimedByUserId()).isEqualTo(42L);
    }

    @Test
    void resolveParticipants_doesNotAutoClaimOpenerWithoutSelfApprove() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireMapping(5L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(100L).build());

        ParticipantSpec spec =
                ParticipantSpec.builder()
                        .role(ParticipantRole.APPROVER)
                        .principalType(PrincipalType.USER)
                        .principalId(42L)
                        .build();

        service.resolveParticipants(
                workflow, 42L, 1L, 5L, List.of(), false, false, null, List.of(spec));

        WorkflowParticipant userApprover =
                workflow.getParticipants().stream()
                        .filter(p -> p.getPrincipalType() == PrincipalType.USER)
                        .findFirst()
                        .orElseThrow();
        assertThat(userApprover.getClaimedByUserId()).isNull();
    }

    @Test
    void resolveParticipants_addsViewerParticipantWithoutClaim() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireMapping(5L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(100L).build());

        ParticipantSpec spec =
                ParticipantSpec.builder()
                        .role(ParticipantRole.VIEWER)
                        .principalType(PrincipalType.USER)
                        .principalId(77L)
                        .slotKey("viewer-77")
                        .build();

        service.resolveParticipants(
                workflow, 1L, 1L, 5L, List.of(), false, false, null, List.of(spec));

        WorkflowParticipant viewer =
                workflow.getParticipants().stream()
                        .filter(p -> p.getRole() == ParticipantRole.VIEWER)
                        .findFirst()
                        .orElseThrow();
        assertThat(viewer.getClaimedByUserId()).isNull();
        assertThat(workflow.getParticipants()).hasSize(2);
    }

    @Test
    void resolveParticipants_throwsOnDuplicateSlotKey() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireMapping(5L, 1L))
                .thenReturn(WorkflowTypeMapping.builder().organizationGroupId(100L).build());

        ParticipantSpec duplicate =
                ParticipantSpec.builder()
                        .role(ParticipantRole.VIEWER)
                        .principalType(PrincipalType.USER)
                        .principalId(1L)
                        .slotKey("custom-slot")
                        .build();

        assertThatThrownBy(
                        () ->
                                service.resolveParticipants(
                                        workflow,
                                        1L,
                                        1L,
                                        5L,
                                        List.of(),
                                        false,
                                        false,
                                        null,
                                        List.of(duplicate, duplicate)))
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("Duplicate workflow slot key");
    }

    private Workflow workflowWithParticipants() {
        return Workflow.builder().tenantId(1L).participants(new LinkedHashSet<>()).build();
    }
}
