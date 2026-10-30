/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.service;

import com.emberone.dutyfunction.dto.OrganizationFunctionOverviewDto;
import com.emberone.dutyfunction.dto.OrganizationFunctionOverviewGroupDto;
import com.emberone.dutyfunction.dto.OrganizationFunctionOverviewMemberDto;
import com.emberone.dutyfunction.dto.ResolvedDutyFunctionDto;
import com.emberone.dutyfunction.standard.FunctionGrant;
import com.emberone.dutyfunction.standard.FunctionGrantRepository;
import com.emberone.dutyfunction.tenant.TenantFunction;
import com.emberone.dutyfunction.tenant.TenantFunctionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.web.list.ListSpecs;
import com.emberone.rank.domain.UserRank;
import com.emberone.rank.dto.UserRankSummaryDto;
import com.emberone.rank.repository.UserRankRepository;
import com.emberone.rank.tenant.TenantRank;
import com.emberone.rank.tenant.TenantRankRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.user.domain.User;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DutyFunctionResolutionService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final UserOrganizationMembershipRepository organizationMembershipRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final FunctionGrantRepository grantRepository;
    private final TenantFunctionRepository functionRepository;
    private final UserRankRepository userRankRepository;
    private final TenantRankRepository tenantRankRepository;

    /**
     * Functions the user holds or held in the organization: one entry per started group membership
     * that maps to a function. A function ends with its group membership.
     */
    @Transactional(readOnly = true)
    public List<ResolvedDutyFunctionDto> resolveForUserInOrganization(
            long userId, long organizationId) {
        Instant now = Instant.now();
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        Optional<Standard> standardOptional =
                effectiveStandardResolver.resolveForOrganization(organization);
        if (standardOptional.isEmpty()) {
            return List.of();
        }
        Standard standard = standardOptional.get();
        List<FunctionGrant> grants = grantRepository.findByStandardId(standard.getId());
        List<UserOrganizationGroupMembership> memberships =
                groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(
                        userId, organizationId);
        List<ResolvedDutyFunctionDto> resolved = new ArrayList<>();
        for (UserOrganizationGroupMembership membership : memberships) {
            if (membership.getStartDate().isAfter(now)) {
                continue;
            }
            OrganizationGroup group = membership.getOrganizationGroup();
            String groupConfigKey = group.getConfigKey();
            if (!StringUtils.hasText(groupConfigKey)) {
                continue;
            }
            Optional<FunctionGrant> grant =
                    resolveGrant(grants, groupConfigKey, membership.getMembershipType());
            if (grant.isEmpty()) {
                continue;
            }
            TenantFunction function =
                    functionRepository
                            .findByConfigKey(grant.get().getFunctionConfigKey())
                            .orElse(null);
            if (function == null
                    || !LifecycleFields.isCatalogSelectable(
                            function.isActive(), function.isArchived())) {
                continue;
            }
            resolved.add(
                    ResolvedDutyFunctionDto.builder()
                            .organizationId(organization.getId())
                            .organizationName(organization.getName())
                            .functionConfigKey(function.getConfigKey())
                            .functionName(function.getName())
                            .functionAbbreviation(function.getAbbreviation())
                            .organizationGroupName(group.getName())
                            .organizationGroupConfigKey(groupConfigKey)
                            .membershipType(membership.getMembershipType())
                            .membershipStartDate(membership.getStartDate())
                            .membershipEndDate(membership.getEndDate())
                            .active(membership.isActiveAt(now))
                            .build());
        }
        return resolved;
    }

    @Transactional(readOnly = true)
    public OrganizationFunctionOverviewDto getOrganizationFunctionOverview(
            long organizationId, String search) {
        Instant now = Instant.now();
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        Optional<Standard> standardOptional =
                effectiveStandardResolver.resolveForOrganization(organization);
        if (standardOptional.isEmpty()) {
            return OrganizationFunctionOverviewDto.builder().groups(List.of()).build();
        }
        Standard standard = standardOptional.get();
        List<FunctionGrant> grants = grantRepository.findByStandardId(standard.getId());
        if (grants.isEmpty()) {
            return OrganizationFunctionOverviewDto.builder().groups(List.of()).build();
        }

        Map<String, OrganizationGroup> groupsByConfigKey =
                organizationGroupRepository.findByOrganizationId(organizationId).stream()
                        .filter(group -> StringUtils.hasText(group.getConfigKey()))
                        .collect(
                                Collectors.toMap(
                                        OrganizationGroup::getConfigKey,
                                        group -> group,
                                        (left, right) -> left,
                                        LinkedHashMap::new));

        Map<Long, Boolean> mainByUserId =
                organizationMembershipRepository
                        .findActiveByOrganization_Id(organizationId, now)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        membership -> membership.getUser().getId(),
                                        membership -> Boolean.TRUE.equals(membership.getMain()),
                                        (left, right) -> left));

        List<Long> userIds = mainByUserId.keySet().stream().sorted().toList();
        Map<Long, UserRank> openRankByUser =
                userIds.isEmpty()
                        ? Map.of()
                        : userRankRepository.findByUserIdInAndEndDateIsNull(userIds).stream()
                                .collect(
                                        Collectors.toMap(
                                                UserRank::getUserId, row -> row, (a, b) -> a));
        Map<Long, TenantRank> ranksById = loadRanks(openRankByUser.values());

        List<OrganizationFunctionOverviewGroupDto> groups = new ArrayList<>();
        for (FunctionGrant grant : grants) {
            OrganizationGroup orgGroup =
                    groupsByConfigKey.get(grant.getOrganizationGroupConfigKey());
            if (orgGroup == null) {
                continue;
            }
            TenantFunction function =
                    functionRepository.findByConfigKey(grant.getFunctionConfigKey()).orElse(null);
            if (function == null
                    || !LifecycleFields.isCatalogSelectable(
                            function.isActive(), function.isArchived())) {
                continue;
            }
            List<OrganizationFunctionOverviewMemberDto> members = new ArrayList<>();
            for (UserOrganizationGroupMembership membership :
                    groupMembershipRepository.findActiveByOrganizationGroup_IdWithUser(
                            orgGroup.getId(), now)) {
                Optional<FunctionGrant> resolvedGrant =
                        resolveGrant(
                                grants,
                                grant.getOrganizationGroupConfigKey(),
                                membership.getMembershipType());
                if (resolvedGrant.isEmpty() || !resolvedGrant.get().getId().equals(grant.getId())) {
                    continue;
                }
                User user = membership.getUser();
                members.add(
                        OrganizationFunctionOverviewMemberDto.builder()
                                .userId(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .mainOrganization(
                                        Boolean.TRUE.equals(mainByUserId.get(user.getId())))
                                .membershipType(membership.getMembershipType())
                                .currentRank(
                                        toRankSummary(openRankByUser.get(user.getId()), ranksById))
                                .build());
            }
            members.sort(
                    Comparator.comparing(OrganizationFunctionOverviewMemberDto::getLastName)
                            .thenComparing(OrganizationFunctionOverviewMemberDto::getFirstName));
            groups.add(
                    OrganizationFunctionOverviewGroupDto.builder()
                            .groupKey("grant:" + grant.getId())
                            .functionConfigKey(function.getConfigKey())
                            .functionName(function.getName())
                            .functionAbbreviation(function.getAbbreviation())
                            .functionSortOrder(function.getSortOrder())
                            .organizationGroupName(orgGroup.getName())
                            .organizationGroupConfigKey(orgGroup.getConfigKey())
                            .grantMembershipType(grant.getMembershipType())
                            .members(members)
                            .build());
        }

        groups.sort(
                Comparator.comparing(OrganizationFunctionOverviewGroupDto::getFunctionSortOrder)
                        .thenComparing(OrganizationFunctionOverviewGroupDto::getFunctionName)
                        .thenComparing(
                                OrganizationFunctionOverviewGroupDto::getOrganizationGroupName));

        return OrganizationFunctionOverviewDto.builder()
                .groups(filterBySearch(groups, search))
                .build();
    }

    /**
     * Case-insensitive token search: a group whose function name / organization group name matches
     * keeps all members; otherwise only members whose first/last name matches are kept, and groups
     * without any match are dropped.
     */
    static List<OrganizationFunctionOverviewGroupDto> filterBySearch(
            List<OrganizationFunctionOverviewGroupDto> groups, String search) {
        List<String> tokens = ListSpecs.searchTokens(search);
        if (tokens.isEmpty()) {
            return groups;
        }
        List<OrganizationFunctionOverviewGroupDto> result = new ArrayList<>();
        for (OrganizationFunctionOverviewGroupDto group : groups) {
            if (matchesAll(
                    tokens, group.getFunctionName() + " " + group.getOrganizationGroupName())) {
                result.add(group);
                continue;
            }
            List<OrganizationFunctionOverviewMemberDto> members =
                    group.getMembers().stream()
                            .filter(
                                    member ->
                                            matchesAll(
                                                    tokens,
                                                    member.getFirstName()
                                                            + " "
                                                            + member.getLastName()))
                            .toList();
            if (!members.isEmpty()) {
                result.add(group.toBuilder().members(members).build());
            }
        }
        return result;
    }

    private static boolean matchesAll(List<String> tokens, String haystack) {
        String lower = haystack.toLowerCase(Locale.ROOT);
        return tokens.stream().allMatch(lower::contains);
    }

    @Transactional(readOnly = true)
    public List<ResolvedDutyFunctionDto> resolveSelfByOrganization(long userId) {
        return groupMembershipRepository.findByUser_Id(userId).stream()
                .map(m -> m.getOrganizationGroup().getOrganization().getId())
                .distinct()
                .flatMap(orgId -> resolveForUserInOrganization(userId, orgId).stream())
                .toList();
    }

    private Map<Long, TenantRank> loadRanks(java.util.Collection<UserRank> rows) {
        Map<Long, TenantRank> ranksById = new LinkedHashMap<>();
        for (UserRank row : rows) {
            Long rankId = row.getTenantRankId();
            if (rankId == null || ranksById.containsKey(rankId)) {
                continue;
            }
            tenantRankRepository.findById(rankId).ifPresent(rank -> ranksById.put(rankId, rank));
        }
        return ranksById;
    }

    private UserRankSummaryDto toRankSummary(UserRank row, Map<Long, TenantRank> ranksById) {
        if (row == null) {
            return null;
        }
        TenantRank rank = ranksById.get(row.getTenantRankId());
        if (rank == null) {
            return null;
        }
        return UserRankSummaryDto.builder()
                .tenantRankId(rank.getId())
                .configKey(rank.getConfigKey())
                .name(rank.getName())
                .abbreviation(rank.getAbbreviation())
                .sortOrder(rank.getSortOrder())
                .startDate(row.getStartDate())
                .build();
    }

    private Optional<FunctionGrant> resolveGrant(
            List<FunctionGrant> grants,
            String organizationGroupConfigKey,
            GroupMembershipType membershipType) {
        return grants.stream()
                .filter(
                        grant ->
                                organizationGroupConfigKey.equals(
                                                grant.getOrganizationGroupConfigKey())
                                        && membershipType.equals(grant.getMembershipType()))
                .findFirst();
    }
}
