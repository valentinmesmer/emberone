/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.service;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.dto.OrganizationGroupPageDto;
import com.emberone.organizationgroup.exception.OrganizationGroupNotFoundException;
import com.emberone.organizationgroup.list.OrganizationGroupListDefinition;
import com.emberone.organizationgroup.list.OrganizationGroupListFilter;
import com.emberone.organizationgroup.list.OrganizationGroupListSpecs;
import com.emberone.organizationgroup.mapper.OrganizationGroupMapper;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationGroupService {

    private final OrganizationGroupRepository organizationGroupRepository;
    private final OrganizationGroupMapper mapper;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;

    public OrganizationGroupDto getGroupById(Long groupId) {
        OrganizationGroup group =
                organizationGroupRepository
                        .findById(groupId)
                        .orElseThrow(() -> new OrganizationGroupNotFoundException(groupId));
        return mapper.toDto(group);
    }

    public List<OrganizationGroupDto> getGroupsByOrganizationId(Long organizationId) {
        return getGroupsByOrganizationId(organizationId, false);
    }

    public List<OrganizationGroupDto> getGroupsByOrganizationId(
            Long organizationId, boolean includeArchived) {
        List<OrganizationGroup> groups =
                includeArchived
                        ? organizationGroupRepository.findAllByOrganizationId(organizationId)
                        : organizationGroupRepository.findByOrganizationId(organizationId);
        return groups.stream()
                .sorted(
                        Comparator.comparing(
                                        OrganizationGroup::getName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(OrganizationGroup::getId))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationGroupPageDto listVisibleGroupsByOrganizationId(
            Long organizationId, ListQuery query, OrganizationGroupListFilter filter) {
        Pageable pageable = OrganizationGroupListDefinition.INSTANCE.pageable(query);
        Specification<OrganizationGroup> spec =
                Specification.allOf(
                        OrganizationGroupListSpecs.forOrganization(organizationId),
                        OrganizationGroupListSpecs.userInterfaceVisible(),
                        OrganizationGroupListSpecs.statusFilter(filter.status()),
                        OrganizationGroupListDefinition.INSTANCE.searchSpecification(query));
        Page<OrganizationGroup> page = organizationGroupRepository.findAll(spec, pageable);
        return OrganizationGroupPageDto.builder()
                .content(page.getContent().stream().map(mapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    public List<OrganizationGroupDto> getVisibleGroupsByOrganizationId(Long organizationId) {
        return getVisibleGroupsByOrganizationId(organizationId, false);
    }

    public List<OrganizationGroupDto> getVisibleGroupsByOrganizationId(
            Long organizationId, boolean includeArchived) {
        return getGroupsByOrganizationId(organizationId, includeArchived).stream()
                .filter(group -> group.getType().isVisibleInUserInterface())
                .toList();
    }

    /**
     * Groups a user belongs to in one organization (from active memberships, UI-visible types
     * only).
     */
    @Transactional(readOnly = true)
    public List<OrganizationGroupDto> getGroupsForUserInOrganization(
            Long userId, Long organizationId, boolean includeArchived) {
        Instant now = Instant.now();
        Map<Long, OrganizationGroup> groupsById = new LinkedHashMap<>();
        for (UserOrganizationGroupMembership membership :
                groupMembershipRepository.findByUser_Id(userId)) {
            OrganizationGroup group = membership.getOrganizationGroup();
            if (!group.getOrganization().getId().equals(organizationId)) {
                continue;
            }
            if (!includeArchived && !membership.isActiveAt(now)) {
                continue;
            }
            if (!includeArchived && group.isArchived()) {
                continue;
            }
            if (!group.getType().isVisibleInUserInterface()
                    && !(includeArchived && group.isArchived())) {
                continue;
            }
            groupsById.putIfAbsent(group.getId(), group);
        }
        return groupsById.values().stream()
                .sorted(
                        Comparator.comparing(
                                        OrganizationGroup::getName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(OrganizationGroup::getId))
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Groups that may receive a new manual membership assignment (visible, non-leadership, active).
     */
    @Transactional(readOnly = true)
    public List<OrganizationGroupDto> getAssignableGroupsByOrganizationId(Long organizationId) {
        return getVisibleGroupsByOrganizationId(organizationId, false).stream()
                .filter(group -> group.getType() != OrganizationGroupType.LEADERSHIP)
                .toList();
    }

    /** Groups that may be selected as appointment audience (visible, active). */
    @Transactional(readOnly = true)
    public List<OrganizationGroupDto> getAudienceGroupsByOrganizationId(Long organizationId) {
        return getVisibleGroupsByOrganizationId(organizationId, false);
    }
}
