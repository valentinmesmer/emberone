/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.standard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.admin.standard.dto.OrgGroupRemovalImpactLevel;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.event.OrganizationGroupDeletingEvent;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.i18n.SystemGroupNamingService;
import com.emberone.standard.domain.Standard;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrganizationGroupStandardDomainTest {

    @Mock OrgGroupTemplateRepository templateRepository;
    @Mock OrganizationGroupRepository groupRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock SystemGroupNamingService systemGroupNamingService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock UserOrganizationGroupMembershipRepository membershipRepository;

    private OrganizationGroupStandardDomain domain;

    private static OrgGroupTemplate customTemplate(long id, String configKey, String name) {
        return OrgGroupTemplate.builder()
                .id(id)
                .tenantId(7L)
                .standardId(1L)
                .configKey(configKey)
                .name(name)
                .groupType(OrganizationGroupType.CUSTOM)
                .build();
    }

    private static OrganizationGroup customInstance(
            Organization org, String configKey, String name, Long originId) {
        return OrganizationGroup.builder()
                .id(42L)
                .organization(org)
                .tenantId(7L)
                .name(name)
                .type(OrganizationGroupType.CUSTOM)
                .configKey(configKey)
                .standardOriginId(originId)
                .build();
    }

    private static Standard standard(long id) {
        return Standard.builder().id(id).tenantId(7L).name("Test").build();
    }

    @BeforeEach
    void setUp() {
        domain =
                new OrganizationGroupStandardDomain(
                        templateRepository,
                        groupRepository,
                        organizationRepository,
                        systemGroupNamingService,
                        eventPublisher,
                        membershipRepository);
    }

    @Test
    void addTemplateRejectsDuplicateSystemGroupType() {
        Standard standard = standard(1L);
        when(templateRepository.existsByStandardIdAndGroupType(
                        1L, OrganizationGroupType.LEADERSHIP))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                domain.addTemplate(
                                        standard,
                                        "Leitung",
                                        null,
                                        OrganizationGroupType.LEADERSHIP,
                                        List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("LEADERSHIP");
        verify(templateRepository, never()).save(any());
    }

    @Test
    void deleteTemplateRejectsSystemGroup() {
        Standard standard = standard(1L);
        OrgGroupTemplate leadership =
                OrgGroupTemplate.builder()
                        .id(10L)
                        .tenantId(7L)
                        .standardId(1L)
                        .configKey("LEADERSHIP")
                        .groupType(OrganizationGroupType.LEADERSHIP)
                        .build();
        when(templateRepository.findByStandardIdAndConfigKey(1L, "LEADERSHIP"))
                .thenReturn(Optional.of(leadership));

        assertThatThrownBy(() -> domain.deleteTemplate(standard, "LEADERSHIP", List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("LEADERSHIP");
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void updateTemplatePropagatesToAllInstances() {
        Standard standard = standard(1L);
        OrgGroupTemplate template = customTemplate(100L, "CUSTOM_x", "Alt");
        when(templateRepository.findByStandardIdAndConfigKey(1L, "CUSTOM_x"))
                .thenReturn(Optional.of(template));
        Organization org = Organization.builder().id(20L).tenantId(7L).build();
        OrganizationGroup instance = customInstance(org, "CUSTOM_x", "Manuell geändert", 100L);
        when(groupRepository.findByOrganizationIdAndConfigKey(20L, "CUSTOM_x"))
                .thenReturn(Optional.of(instance));

        domain.updateTemplate(standard, "CUSTOM_x", "Neu", null, List.of(org));

        ArgumentCaptor<OrganizationGroup> captor = ArgumentCaptor.forClass(OrganizationGroup.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Neu");
    }

    @Test
    void deleteTemplateHardDeletesWhenNoMembershipHistory() {
        Standard standard = standard(1L);
        OrgGroupTemplate template = customTemplate(100L, "CUSTOM_x", "Crew");
        when(templateRepository.findByStandardIdAndConfigKey(1L, "CUSTOM_x"))
                .thenReturn(Optional.of(template));
        Organization org = Organization.builder().id(20L).tenantId(7L).build();
        OrganizationGroup instance = customInstance(org, "CUSTOM_x", "Crew", 100L);
        when(groupRepository.findByOrganizationIdAndConfigKey(20L, "CUSTOM_x"))
                .thenReturn(Optional.of(instance));
        when(membershipRepository.existsByOrganizationGroup_IdIn(List.of(42L))).thenReturn(false);

        domain.deleteTemplate(standard, "CUSTOM_x", List.of(org));

        verify(groupRepository).delete(instance);
        verify(eventPublisher).publishEvent(new OrganizationGroupDeletingEvent(42L, 7L));
        verify(templateRepository).delete(template);
    }

    @Test
    void deleteTemplateArchivesWhenMembershipHistoryExists() {
        Standard standard = standard(1L);
        OrgGroupTemplate template = customTemplate(100L, "CUSTOM_x", "Crew");
        when(templateRepository.findByStandardIdAndConfigKey(1L, "CUSTOM_x"))
                .thenReturn(Optional.of(template));
        Organization org = Organization.builder().id(20L).tenantId(7L).name("FW 1").build();
        OrganizationGroup instance = customInstance(org, "CUSTOM_x", "Crew", 100L);
        when(groupRepository.findByOrganizationIdAndConfigKey(20L, "CUSTOM_x"))
                .thenReturn(Optional.of(instance));
        when(membershipRepository.existsByOrganizationGroup_IdIn(List.of(42L))).thenReturn(true);

        domain.deleteTemplate(standard, "CUSTOM_x", List.of(org));

        ArgumentCaptor<OrganizationGroup> captor = ArgumentCaptor.forClass(OrganizationGroup.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().isArchived()).isTrue();
        assertThat(captor.getValue().getConfigKey()).isNull();
        assertThat(captor.getValue().getStandardOriginId()).isNull();
        verify(eventPublisher).publishEvent(new OrganizationGroupDeletingEvent(42L, 7L));
        verify(groupRepository, never()).delete(instance);
    }

    @Test
    void deleteTemplateEndsActiveMembershipsBeforeArchive() {
        Standard standard = standard(1L);
        OrgGroupTemplate template = customTemplate(100L, "CUSTOM_x", "Crew");
        when(templateRepository.findByStandardIdAndConfigKey(1L, "CUSTOM_x"))
                .thenReturn(Optional.of(template));
        Organization org = Organization.builder().id(20L).tenantId(7L).build();
        OrganizationGroup instance = customInstance(org, "CUSTOM_x", "Crew", 100L);
        when(groupRepository.findByOrganizationIdAndConfigKey(20L, "CUSTOM_x"))
                .thenReturn(Optional.of(instance));
        when(membershipRepository.existsByOrganizationGroup_IdIn(List.of(42L))).thenReturn(true);

        domain.deleteTemplate(standard, "CUSTOM_x", List.of(org));

        verify(membershipRepository)
                .endActiveMembershipsByGroupIds(eq(List.of(42L)), any(Instant.class));
        verify(groupRepository).save(any());
    }

    @Test
    void previewRemovalClassifiesActiveImpact() {
        Standard standard = standard(1L);
        OrgGroupTemplate template = customTemplate(100L, "CUSTOM_x", "Crew");
        when(templateRepository.findByStandardIdAndConfigKey(1L, "CUSTOM_x"))
                .thenReturn(Optional.of(template));
        Organization org = Organization.builder().id(20L).tenantId(7L).name("FW 1").build();
        OrganizationGroup instance = customInstance(org, "CUSTOM_x", "Crew", 100L);
        when(groupRepository.findByOrganizationIdAndConfigKey(20L, "CUSTOM_x"))
                .thenReturn(Optional.of(instance));
        when(membershipRepository.existsByOrganizationGroup_IdIn(List.of(42L))).thenReturn(true);
        when(membershipRepository.findActiveUserIdsByGroupIds(eq(List.of(42L)), any(Instant.class)))
                .thenReturn(List.of(1L, 2L));

        var impact = domain.previewRemoval(standard, "CUSTOM_x", List.of(org));

        assertThat(impact.getLevel()).isEqualTo(OrgGroupRemovalImpactLevel.ACTIVE);
        assertThat(impact.getTotalActiveMemberships()).isEqualTo(2);
        assertThat(impact.getTotalOrgsAffected()).isEqualTo(1);
    }

    @Test
    void reassignRebasesExistingInstanceToNewStandard() {
        Standard target = standard(2L);
        when(templateRepository.existsByStandardIdAndGroupType(
                        2L, OrganizationGroupType.LEADERSHIP))
                .thenReturn(true);
        when(templateRepository.existsByStandardIdAndGroupType(
                        2L, OrganizationGroupType.ALL_MEMBERS))
                .thenReturn(true);
        Organization org = Organization.builder().id(20L).tenantId(7L).build();
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(org));
        OrgGroupTemplate newTemplate = customTemplate(200L, "CUSTOM_x", "Neu");
        when(templateRepository.findByStandardId(2L)).thenReturn(List.of(newTemplate));
        OrganizationGroup instance = customInstance(org, "CUSTOM_x", "Alt", 100L);
        when(groupRepository.findByOrganizationIdAndConfigKey(20L, "CUSTOM_x"))
                .thenReturn(Optional.of(instance));
        when(groupRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(20L))
                .thenReturn(List.of());

        domain.reassignForOrg(20L, target);

        ArgumentCaptor<OrganizationGroup> captor = ArgumentCaptor.forClass(OrganizationGroup.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Neu");
        assertThat(captor.getValue().getStandardOriginId()).isEqualTo(200L);
    }

    @Test
    void requireWorkflowApproverOrgGroupRejectsAllMembers() {
        Standard standard = standard(1L);
        OrgGroupTemplate allMembers =
                OrgGroupTemplate.builder()
                        .id(11L)
                        .tenantId(7L)
                        .standardId(1L)
                        .configKey("ALL_MEMBERS")
                        .groupType(OrganizationGroupType.ALL_MEMBERS)
                        .build();
        when(templateRepository.findByStandardIdAndConfigKey(1L, "ALL_MEMBERS"))
                .thenReturn(Optional.of(allMembers));

        assertThatThrownBy(() -> domain.requireWorkflowApproverOrgGroup(standard, "ALL_MEMBERS"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be used as a workflow approver");
    }
}
