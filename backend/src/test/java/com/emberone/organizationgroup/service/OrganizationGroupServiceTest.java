/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.mapper.OrganizationGroupMapper;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationGroupServiceTest {

    @Mock OrganizationGroupRepository groupRepository;
    @Mock OrganizationGroupMapper mapper;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;

    private OrganizationGroupService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationGroupService(groupRepository, mapper, groupMembershipRepository);
    }

    @Test
    void listsGroupsSortedAndHidesAllMembersFromVisibleList() {
        OrganizationGroup zulu = group(3L, "Zulu", OrganizationGroupType.CUSTOM);
        OrganizationGroup all = group(2L, "Alle Mitglieder", OrganizationGroupType.ALL_MEMBERS);
        OrganizationGroup alpha = group(1L, "Alpha", OrganizationGroupType.CUSTOM);
        when(groupRepository.findByOrganizationId(10L)).thenReturn(List.of(zulu, all, alpha));
        mockDto(zulu);
        mockDto(all);
        mockDto(alpha);

        assertThat(service.getGroupsByOrganizationId(10L))
                .extracting(OrganizationGroupDto::getName)
                .containsExactly("Alle Mitglieder", "Alpha", "Zulu");
        assertThat(service.getVisibleGroupsByOrganizationId(10L))
                .extracting(OrganizationGroupDto::getName)
                .containsExactly("Alpha", "Zulu");
    }

    @Test
    void listsGroupsIncludingArchivedWhenRequested() {
        OrganizationGroup active = group(1L, "Active", OrganizationGroupType.CUSTOM);
        OrganizationGroup archived = group(2L, "Archived", OrganizationGroupType.CUSTOM);
        archived.setArchived(true);
        when(groupRepository.findAllByOrganizationId(10L)).thenReturn(List.of(active, archived));
        mockDto(active);
        mockDto(archived);

        assertThat(service.getGroupsByOrganizationId(10L, true))
                .extracting(OrganizationGroupDto::getName)
                .containsExactly("Active", "Archived");
        assertThat(service.getVisibleGroupsByOrganizationId(10L, true))
                .extracting(OrganizationGroupDto::getName)
                .containsExactly("Active", "Archived");
    }

    @Test
    void listsGroupsIncludingArchivedButHidesAllMembers() {
        OrganizationGroup active = group(1L, "Active", OrganizationGroupType.CUSTOM);
        OrganizationGroup allMembers =
                group(2L, "Alle Mitglieder", OrganizationGroupType.ALL_MEMBERS);
        when(groupRepository.findAllByOrganizationId(10L)).thenReturn(List.of(active, allMembers));
        mockDto(active);
        mockDto(allMembers);

        assertThat(service.getVisibleGroupsByOrganizationId(10L, true))
                .extracting(OrganizationGroupDto::getName)
                .containsExactly("Active");
    }

    @Test
    void getGroupByIdReturnsArchivedGroup() {
        OrganizationGroup archived = group(5L, "Old Crew", OrganizationGroupType.CUSTOM);
        archived.setArchived(true);
        OrganizationGroupDto dto =
                OrganizationGroupDto.builder().id(5L).name("Old Crew").archived(true).build();
        when(groupRepository.findById(5L)).thenReturn(Optional.of(archived));
        when(mapper.toDto(archived)).thenReturn(dto);

        assertThat(service.getGroupById(5L).isArchived()).isTrue();
    }

    private OrganizationGroup group(long id, String name, OrganizationGroupType type) {
        return OrganizationGroup.builder()
                .id(id)
                .tenantId(7L)
                .name(name)
                .type(type)
                .organization(Organization.builder().id(10L).organizationCode("FW").build())
                .build();
    }

    private void mockDto(OrganizationGroup group) {
        OrganizationGroupDto dto =
                OrganizationGroupDto.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .type(group.getType())
                        .archived(group.isArchived())
                        .build();
        when(mapper.toDto(group)).thenReturn(dto);
    }
}
