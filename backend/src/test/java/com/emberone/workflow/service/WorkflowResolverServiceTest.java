/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.ADDITIONAL;
import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.CONTEXT;
import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.HOME_ORG;
import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.NEW_MAIN_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.emberone.workflow.domain.*;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.spi.ApproverOrg;
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

        when(mappingService.requireSlot(8L, 2L, ADDITIONAL)).thenReturn(slot(200L, false));

        WorkflowResolverService.OrgApproverSlotSyncResult result =
                service.syncOrgApproverSlots(workflow, 2L, 5L, List.of(8L), true, List.of());

        assertThat(result.changed()).isTrue();
        assertThat(result.removedPendingParticipants()).containsExactly(obsolete);
        assertThat(workflow.getParticipants())
                .extracting(WorkflowParticipant::getSlotKey)
                .containsExactly("AGR:8");
    }

    @Test
    void resolveApproverOrganizationIds_collapsesIdenticalContextAndDynamicOrganizationToContext() {
        // Course registration: offering organization (context) and the user's home organization
        // (dynamic) are the same organization, so a single decision with the CONTEXT settings.
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L, List.of(), false, List.of(new ApproverOrg(5L, HOME_ORG))))
                .containsExactly(entry(5L, CONTEXT));
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L, List.of(), false, List.of(new ApproverOrg(6L, HOME_ORG))))
                .containsExactly(entry(5L, CONTEXT), entry(6L, HOME_ORG));
    }

    @Test
    void resolveApproverOrganizationIds_ordersContextDynamicThenAdditionalAndFirstRoleWins() {
        // 7 is a dynamic org and also an extra org: the dynamic role wins over ADDITIONAL.
        // 5 is the context org and an extra org: CONTEXT wins.
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L,
                                List.of(5L, 7L, 8L),
                                false,
                                List.of(new ApproverOrg(7L, NEW_MAIN_ORG))))
                .containsExactly(
                        entry(5L, CONTEXT), entry(7L, NEW_MAIN_ORG), entry(8L, ADDITIONAL));
    }

    @Test
    void resolveApproverOrganizationIds_exclusiveStaticOrgsAreAdditionalExceptTheContextOrg() {
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L,
                                List.of(8L, 5L),
                                true,
                                List.of(new ApproverOrg(7L, NEW_MAIN_ORG))))
                .containsExactly(entry(8L, ADDITIONAL), entry(5L, CONTEXT));
        // Exclusive without extra orgs falls back to the normal resolution.
        assertThat(
                        WorkflowResolverService.resolveApproverOrganizationIds(
                                5L, List.of(), true, List.of()))
                .containsExactly(entry(5L, CONTEXT));
    }

    @Test
    void resolveParticipants_autoApproveAndGroupComeFromTheRoleOfEachSlot() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));
        when(mappingService.requireSlot(9L, 1L, HOME_ORG)).thenReturn(slot(200L, true));
        when(mappingService.requireSlot(8L, 1L, ADDITIONAL)).thenReturn(slot(300L, true));

        service.resolveParticipants(
                workflow,
                1L,
                1L,
                5L,
                List.of(8L),
                false,
                false,
                List.of(new ApproverOrg(9L, HOME_ORG)),
                null);

        assertThat(workflow.getParticipants())
                .extracting(
                        WorkflowParticipant::getSlotKey,
                        WorkflowParticipant::getSlotRole,
                        WorkflowParticipant::getPrincipalId,
                        WorkflowParticipant::getSlotStatus)
                .containsExactly(
                        tuple("AGR:5", CONTEXT, 100L, ParticipantSlotStatus.PENDING),
                        tuple("AGR:9", HOME_ORG, 200L, ParticipantSlotStatus.APPROVED),
                        tuple("AGR:8", ADDITIONAL, 300L, ParticipantSlotStatus.APPROVED));
    }

    @Test
    void syncOrgApproverSlots_updatesTheRoleOfAnExistingSlotWhenItChanged() {
        Workflow workflow = workflowWithParticipants();
        WorkflowParticipant existing = approverSlot(workflow, 7L, CONTEXT);
        workflow.getParticipants().add(existing);
        when(mappingService.requireSlot(5L, 2L, CONTEXT)).thenReturn(slot(100L, false));

        WorkflowResolverService.OrgApproverSlotSyncResult result =
                service.syncOrgApproverSlots(
                        workflow,
                        2L,
                        5L,
                        List.of(),
                        false,
                        List.of(new ApproverOrg(7L, NEW_MAIN_ORG)));

        // 5 is new (added), 7 keeps its slot but now decides as NEW_MAIN_ORG.
        assertThat(result.changed()).isTrue();
        assertThat(existing.getSlotRole()).isEqualTo(NEW_MAIN_ORG);
        assertThat(workflow.getParticipants())
                .extracting(WorkflowParticipant::getSlotKey)
                .containsExactlyInAnyOrder("AGR:5", "AGR:7");
        // The pending slot changed its role, so the caller applies the new role's settings.
        assertThat(result.reroledPendingParticipants()).containsExactly(existing);
    }

    @Test
    void syncOrgApproverSlots_doesNotReportRoleChangesOfDecidedSlots() {
        Workflow workflow = workflowWithParticipants();
        WorkflowParticipant decided = approverSlot(workflow, 7L, CONTEXT);
        decided.setSlotStatus(ParticipantSlotStatus.APPROVED);
        workflow.getParticipants().add(decided);
        when(mappingService.requireSlot(5L, 2L, CONTEXT)).thenReturn(slot(100L, false));

        WorkflowResolverService.OrgApproverSlotSyncResult result =
                service.syncOrgApproverSlots(
                        workflow,
                        2L,
                        5L,
                        List.of(),
                        false,
                        List.of(new ApproverOrg(7L, NEW_MAIN_ORG)));

        assertThat(decided.getSlotRole()).isEqualTo(NEW_MAIN_ORG);
        assertThat(result.reroledPendingParticipants()).isEmpty();
    }

    @Test
    void applySlotRoles_setsTheExpectedRoleWithoutAddingOrRemovingSlots() {
        Workflow workflow = workflowWithParticipants();
        WorkflowParticipant context = approverSlot(workflow, 5L, CONTEXT);
        WorkflowParticipant dynamic = approverSlot(workflow, 7L, CONTEXT);
        WorkflowParticipant extra = approverSlot(workflow, 8L, CONTEXT);
        WorkflowParticipant unknown = approverSlot(workflow, 9L, CONTEXT);
        workflow.getParticipants().addAll(List.of(context, dynamic, extra, unknown));

        int updated =
                service.applySlotRoles(
                        workflow,
                        5L,
                        List.of(8L),
                        false,
                        List.of(new ApproverOrg(7L, NEW_MAIN_ORG)));

        assertThat(updated).isEqualTo(2);
        assertThat(context.getSlotRole()).isEqualTo(CONTEXT);
        assertThat(dynamic.getSlotRole()).isEqualTo(NEW_MAIN_ORG);
        assertThat(extra.getSlotRole()).isEqualTo(ADDITIONAL);
        assertThat(unknown.getSlotRole()).isEqualTo(CONTEXT);
        assertThat(workflow.getParticipants()).hasSize(4);
    }

    @Test
    void resolveParticipants_createsApproverForContextOrganization() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));

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
        when(mappingService.requireSlot(8L, 2L, ADDITIONAL)).thenReturn(slot(200L, true));

        service.resolveParticipants(
                workflow,
                1L,
                2L,
                5L,
                List.of(8L),
                true,
                false,
                List.of(new ApproverOrg(9L, NEW_MAIN_ORG)),
                null);

        assertThat(workflow.getParticipants()).hasSize(1);
        WorkflowParticipant participant = workflow.getParticipants().iterator().next();
        assertThat(participant.getSlotKey()).isEqualTo("AGR:8");
        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.APPROVED);
    }

    @Test
    void resolveParticipants_autoClaimsUserApproverWhenOpenedByDifferentUser() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));

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
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));
        when(mappingService.requireSlot(9L, 1L, NEW_MAIN_ORG)).thenReturn(slot(200L, false));

        service.resolveParticipants(
                workflow,
                1L,
                1L,
                5L,
                List.of(),
                false,
                false,
                List.of(new ApproverOrg(9L, NEW_MAIN_ORG)),
                null);

        assertThat(workflow.getParticipants())
                .extracting(WorkflowParticipant::getSlotKey, WorkflowParticipant::getSlotRole)
                .containsExactly(tuple("AGR:5", CONTEXT), tuple("AGR:9", NEW_MAIN_ORG));
    }

    @Test
    void resolveParticipants_autoClaimsOpenerWhenSelfApproveAllowed() {
        Workflow workflow = workflowWithParticipants();
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));

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
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));

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
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));

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
        when(mappingService.requireSlot(5L, 1L, CONTEXT)).thenReturn(slot(100L, false));

        ParticipantSpec duplicate =
                ParticipantSpec.builder()
                        .role(ParticipantRole.VIEWER)
                        .principalType(PrincipalType.USER)
                        .principalId(1L)
                        .slotKey("custom-slot")
                        .build();

        assertThatThrownBy(() ->
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

    private static WorkflowTypeMappingSlot slot(Long groupId, boolean autoApprove) {
        return WorkflowTypeMappingSlot.builder()
                .organizationGroupId(groupId)
                .autoApprove(autoApprove)
                .build();
    }

    private static WorkflowParticipant approverSlot(Workflow workflow, Long orgId, String role) {
        return WorkflowParticipant.builder()
                .tenantId(1L)
                .workflow(workflow)
                .role(ParticipantRole.APPROVER)
                .requirement(ParticipantRequirement.MANDATORY)
                .principalType(PrincipalType.ORGANIZATION_GROUP)
                .principalId(100L)
                .organizationId(orgId)
                .slotKey("AGR:" + orgId)
                .slotRole(role)
                .slotStatus(ParticipantSlotStatus.PENDING)
                .build();
    }

    private Workflow workflowWithParticipants() {
        return Workflow.builder().tenantId(1L).participants(new LinkedHashSet<>()).build();
    }
}
