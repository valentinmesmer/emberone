/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.service;

import com.emberone.admin.organization.dto.*;
import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.management.service.ManagementCapabilityService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.CreateChildOrganizationRequest;
import com.emberone.organization.dto.DeactivateOrganizationRequest;
import com.emberone.organization.dto.MoveOrganizationRequest;
import com.emberone.organization.dto.UpdateOrganizationRequest;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationDeactivationService;
import com.emberone.organization.service.OrganizationService;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.dto.CreateUserRequest;
import com.emberone.user.dto.SetupUserRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.dto.UserPageDto;
import com.emberone.user.dto.UserSummaryDto;
import com.emberone.user.mapper.UserMapper;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UpdateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipNotFoundException;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Administrative organization hierarchy, metadata, and leadership use cases. */
@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private static final Comparator<AdminOrganizationTreeNodeDto> TREE_NODE_ORDER =
            Comparator.comparing(
                            AdminOrganizationTreeNodeDto::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AdminOrganizationTreeNodeDto::getId);

    private final TenantRepository tenantRepository;
    private final OrganizationService organizationService;
    private final OrganizationDeactivationService organizationDeactivationService;
    private final OrganizationRepository organizationRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final UserOrganizationGroupMembershipService groupMembershipService;
    private final UserOrganizationGroupMembershipMapper groupMembershipMapper;
    private final ManagementCapabilityService managementCapabilityService;

    @Transactional(readOnly = true)
    public AdminOrganizationTreeResponseDto getOrganizationTree(long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        List<Organization> organizations = organizationRepository.findAllWithParentFetch();
        Set<Long> organizationsWithLeader =
                findOrganizationIdsSatisfyingLeaderRequirement(organizations);
        Map<Long, Boolean> leaderMissingByOrganization =
                buildLeaderMissingInSubtreeMap(organizations);

        Map<Long, AdminOrganizationTreeNodeDto> nodesById = new LinkedHashMap<>();
        for (Organization organization : organizations) {
            nodesById.put(
                    organization.getId(),
                    AdminOrganizationTreeNodeDto.builder()
                            .id(organization.getId())
                            .name(organization.getName())
                            .organizationCode(organization.getOrganizationCode())
                            .organizationTypeId(organization.getType().getId())
                            .leaderMissing(!organizationsWithLeader.contains(organization.getId()))
                            .leaderMissingInSubtree(
                                    leaderMissingByOrganization.getOrDefault(
                                            organization.getId(), false))
                            .deactivatedAt(organization.getDeactivatedAt())
                            .children(new ArrayList<>())
                            .build());
        }

        List<AdminOrganizationTreeNodeDto> roots = new ArrayList<>();
        for (Organization organization : organizations) {
            AdminOrganizationTreeNodeDto node = nodesById.get(organization.getId());
            if (organization.getParentOrganization() == null) {
                roots.add(node);
            } else {
                AdminOrganizationTreeNodeDto parent =
                        nodesById.get(organization.getParentOrganization().getId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        sortTree(roots);

        return AdminOrganizationTreeResponseDto.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .roots(roots)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminOrganizationDetailDto> getChildOrganizations(
            long tenantId, long organizationId) {
        requireOrganizationInTenant(tenantId, organizationId);
        Map<Long, Boolean> leaderMissingByOrganization = buildLeaderMissingInSubtreeMap();
        Set<Long> managementCandidates = managementCapabilityService.managementCandidateIds();
        return organizationRepository.findByParentOrganizationId(organizationId).stream()
                .sorted(
                        Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(Organization::getId))
                .map(child -> toDetail(child, leaderMissingByOrganization, managementCandidates))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrganizationDetailDto getOrganization(long tenantId, long organizationId) {
        Organization organization = requireOrganizationInTenant(tenantId, organizationId);
        return toDetail(
                organization,
                buildLeaderMissingInSubtreeMap(),
                managementCapabilityService.managementCandidateIds());
    }

    @Transactional(readOnly = true)
    public UserPageDto getOrganizationUsers(long tenantId, long organizationId, ListQuery query) {
        requireOrganizationInTenant(tenantId, organizationId);
        return userService.listUsersForOrganization(organizationId, query);
    }

    @Transactional
    public AdminOrganizationDetailDto moveOrganization(
            long tenantId, long organizationId, MoveOrganizationRequest request) {
        organizationService.moveOrganization(tenantId, organizationId, request);
        return getOrganization(tenantId, organizationId);
    }

    @Transactional
    public void deactivateOrganization(
            long tenantId, long organizationId, DeactivateOrganizationRequest request) {
        organizationDeactivationService.deactivateOrganization(tenantId, organizationId, request);
    }

    @Transactional
    public AdminOrganizationDetailDto createChildOrganization(
            long tenantId, long parentOrganizationId, CreateChildOrganizationRequest request) {
        requireActiveOrganizationInTenant(tenantId, parentOrganizationId);
        Long createdId =
                organizationService.createChildOrganization(parentOrganizationId, request).getId();
        Organization created =
                organizationRepository
                        .findById(createdId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Created organization not found"));
        return toDetail(
                created,
                buildLeaderMissingInSubtreeMap(),
                managementCapabilityService.managementCandidateIds());
    }

    @Transactional
    public AdminOrganizationDetailDto updateOrganization(
            long tenantId, long organizationId, UpdateOrganizationRequest request) {
        requireActiveOrganizationInTenant(tenantId, organizationId);
        organizationService.updateOrganization(organizationId, request);
        Organization updated =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Organization not found after update"));
        return toDetail(
                updated,
                buildLeaderMissingInSubtreeMap(),
                managementCapabilityService.managementCandidateIds());
    }

    @Transactional
    public AdminOrganizationDetailDto createOrganizationLeaderUser(
            long tenantId, long organizationId, CreateUserRequest request) {
        requireActiveOrganizationInTenant(tenantId, organizationId);
        requireNoActiveLeader(organizationId);

        SetupUserRequest setup =
                SetupUserRequest.builder()
                        .salutation(request.getSalutation())
                        .firstName(request.getFirstName().trim())
                        .lastName(request.getLastName().trim())
                        .dateOfBirth(request.getDateOfBirth())
                        .privateEmail(request.getPrivateEmail().trim())
                        .street(request.getStreet().trim())
                        .city(request.getCity().trim())
                        .postalCode(request.getPostalCode().trim())
                        .country(request.getCountry().trim())
                        .mainOrganizationId(organizationId)
                        .build();
        UserDto created = userService.createInitialTenantMember(tenantId, setup);
        userService.addUserToLeadershipGroupAsLeader(created.getId(), organizationId);
        return getOrganization(tenantId, organizationId);
    }

    @Transactional(readOnly = true)
    public AdminLeadershipGroupMembershipsDto getLeadershipGroupMemberships(
            long tenantId, long organizationId) {
        requireOrganizationInTenant(tenantId, organizationId);
        OrganizationGroup leadershipGroup = requireLeadershipGroup(organizationId);
        List<UserOrganizationGroupMembershipDto> memberships =
                groupMembershipRepository
                        .findByOrganizationGroup_IdWithUser(leadershipGroup.getId())
                        .stream()
                        .map(groupMembershipMapper::toDto)
                        .toList();
        return AdminLeadershipGroupMembershipsDto.builder()
                .leadershipGroupId(leadershipGroup.getId())
                .memberships(memberships)
                .build();
    }

    @Transactional
    public UserOrganizationGroupMembershipDto createLeadershipGroupMembership(
            long tenantId,
            long organizationId,
            CreateAdminLeadershipGroupMembershipRequest request) {
        requireActiveOrganizationInTenant(tenantId, organizationId);
        userService.ensureOrganizationMembershipAndAllMembersGroup(
                tenantId, request.getUserId(), organizationId);
        OrganizationGroup leadershipGroup = requireLeadershipGroup(organizationId);
        CreateUserOrganizationGroupMembershipRequest membershipRequest =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(request.getUserId())
                        .organizationGroupId(leadershipGroup.getId())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .membershipType(request.getMembershipType())
                        .build();
        return groupMembershipService.createSystemManagedMembership(membershipRequest);
    }

    @Transactional
    public UserOrganizationGroupMembershipDto updateLeadershipGroupMembership(
            long tenantId,
            long organizationId,
            long membershipId,
            UpdateUserOrganizationGroupMembershipRequest request) {
        requireActiveOrganizationInTenant(tenantId, organizationId);
        requireLeadershipMembership(organizationId, membershipId);
        return groupMembershipService.updateSystemManagedMembership(membershipId, request);
    }

    @Transactional(readOnly = true)
    public List<RootOrganizationSummaryDto> getRootOrganizationSummaries() {
        Map<Long, Boolean> leaderMissingByOrganization = buildLeaderMissingInSubtreeMap();
        return organizationRepository.findByParentOrganizationIsNull().stream()
                .map(organization -> toRootSummary(organization, leaderMissingByOrganization))
                .sorted(
                        Comparator.comparing(
                                        RootOrganizationSummaryDto::getName,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(RootOrganizationSummaryDto::getId))
                .toList();
    }

    private Tenant requireTenant(long tenantId) {
        return tenantRepository
                .findById(tenantId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    private Organization requireOrganizationInTenant(long tenantId, long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Organization not found"));
        if (!organization.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Organization does not belong to this tenant");
        }
        return organization;
    }

    private Organization requireActiveOrganizationInTenant(long tenantId, long organizationId) {
        Organization organization = requireOrganizationInTenant(tenantId, organizationId);
        OrganizationDeactivationService.requireActive(organization);
        return organization;
    }

    private void requireNoActiveLeader(long organizationId) {
        if (!groupMembershipRepository
                .findActiveLeadershipLeaderMemberships(
                        organizationId, GroupMembershipType.LEADER, Instant.now())
                .isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Organization already has an active leader");
        }
    }

    private OrganizationGroup requireLeadershipGroup(long organizationId) {
        return organizationGroupRepository
                .findByOrganizationIdAndType(organizationId, OrganizationGroupType.LEADERSHIP)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Leadership group not found for organization"));
    }

    private UserOrganizationGroupMembership requireLeadershipMembership(
            long organizationId, long membershipId) {
        OrganizationGroup leadershipGroup = requireLeadershipGroup(organizationId);
        UserOrganizationGroupMembership membership =
                groupMembershipRepository
                        .findById(membershipId)
                        .orElseThrow(
                                () ->
                                        new UserOrganizationGroupMembershipNotFoundException(
                                                membershipId));
        if (!membership.getOrganizationGroup().getId().equals(leadershipGroup.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Membership does not belong to the leadership group");
        }
        return membership;
    }

    private void sortTree(List<AdminOrganizationTreeNodeDto> nodes) {
        nodes.sort(TREE_NODE_ORDER);
        nodes.forEach(node -> sortTree(node.getChildren()));
    }

    private Set<Long> findOrganizationIdsWithActiveLeader() {
        return new HashSet<>(
                groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(
                        Instant.now()));
    }

    /**
     * Organizations that do not need a leader check: those with an active leader and deactivated
     * ones, since a deactivated unit is no longer led and must not be reported as missing one.
     */
    private Set<Long> findOrganizationIdsSatisfyingLeaderRequirement(
            List<Organization> organizations) {
        Set<Long> satisfying = findOrganizationIdsWithActiveLeader();
        for (Organization organization : organizations) {
            if (organization.getDeactivatedAt() != null) {
                satisfying.add(organization.getId());
            }
        }
        return satisfying;
    }

    private Map<Long, Boolean> buildLeaderMissingInSubtreeMap() {
        return buildLeaderMissingInSubtreeMap(organizationRepository.findAllWithParentFetch());
    }

    private Map<Long, Boolean> buildLeaderMissingInSubtreeMap(List<Organization> organizations) {
        if (organizations.isEmpty()) {
            return Map.of();
        }
        Set<Long> organizationsWithLeader =
                findOrganizationIdsSatisfyingLeaderRequirement(organizations);
        Map<Long, List<Long>> childrenByParentId = new HashMap<>();
        for (Organization organization : organizations) {
            if (organization.getParentOrganization() != null) {
                childrenByParentId
                        .computeIfAbsent(
                                organization.getParentOrganization().getId(),
                                ignored -> new ArrayList<>())
                        .add(organization.getId());
            }
        }

        Map<Long, Boolean> memo = new HashMap<>();
        for (Organization organization : organizations) {
            computeLeaderMissingInSubtree(
                    organization.getId(), childrenByParentId, organizationsWithLeader, memo);
        }
        return memo;
    }

    private boolean computeLeaderMissingInSubtree(
            long organizationId,
            Map<Long, List<Long>> childrenByParentId,
            Set<Long> organizationsWithLeader,
            Map<Long, Boolean> memo) {
        Boolean cached = memo.get(organizationId);
        if (cached != null) {
            return cached;
        }
        boolean missing = !organizationsWithLeader.contains(organizationId);
        if (!missing) {
            missing =
                    childrenByParentId.getOrDefault(organizationId, List.of()).stream()
                            .anyMatch(
                                    childId ->
                                            computeLeaderMissingInSubtree(
                                                    childId,
                                                    childrenByParentId,
                                                    organizationsWithLeader,
                                                    memo));
        }
        memo.put(organizationId, missing);
        return missing;
    }

    private AdminOrganizationDetailDto toDetail(
            Organization organization,
            Map<Long, Boolean> leaderMissingByOrganization,
            Set<Long> managementCandidates) {
        List<UserOrganizationGroupMembership> leaders =
                groupMembershipRepository.findActiveLeadershipMemberships(
                        organization.getId(), Instant.now());
        UserSummaryDto leader =
                leaders.isEmpty() ? null : userMapper.toSummaryDto(leaders.getFirst().getUser());
        return AdminOrganizationDetailDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .organizationCode(organization.getOrganizationCode())
                .organizationTypeId(organization.getType().getId())
                .parentOrganizationId(
                        organization.getParentOrganization() == null
                                ? null
                                : organization.getParentOrganization().getId())
                .leader(leader)
                .hasChildren(
                        organizationRepository.existsByParentOrganization_Id(organization.getId()))
                .leaderMissingInSubtree(
                        leaderMissingByOrganization.getOrDefault(
                                organization.getId(), leaders.isEmpty()))
                .managementCapable(
                        managementCandidates.contains(organization.getId())
                                && managementCapabilityService.isCapable(organization))
                .deactivatedAt(organization.getDeactivatedAt())
                .build();
    }

    private RootOrganizationSummaryDto toRootSummary(
            Organization organization, Map<Long, Boolean> leaderMissingByOrganization) {
        List<UserOrganizationGroupMembership> leaders =
                groupMembershipRepository.findActiveLeadershipMemberships(
                        organization.getId(), Instant.now());
        UserSummaryDto leader =
                leaders.isEmpty() ? null : userMapper.toSummaryDto(leaders.getFirst().getUser());
        return RootOrganizationSummaryDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .organizationCode(organization.getOrganizationCode())
                .organizationTypeId(organization.getType().getId())
                .leader(leader)
                .hasChildren(
                        organizationRepository.existsByParentOrganization_Id(organization.getId()))
                .leaderMissingInSubtree(
                        leaderMissingByOrganization.getOrDefault(
                                organization.getId(), leaders.isEmpty()))
                .build();
    }
}
