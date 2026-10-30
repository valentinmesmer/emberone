/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.service;

import com.emberone.platform.exception.AccessDeniedException;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.rank.domain.UserRank;
import com.emberone.rank.dto.*;
import com.emberone.rank.list.OrganizationRankMemberListDefinition;
import com.emberone.rank.list.OrganizationRankMemberListSpecs;
import com.emberone.rank.mapper.OrganizationRankMemberDtoAssembler;
import com.emberone.rank.repository.UserRankRepository;
import com.emberone.rank.tenant.TenantRank;
import com.emberone.rank.tenant.TenantRankRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserRankService {

    private final UserRankRepository userRankRepository;
    private final TenantRankRepository tenantRankRepository;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final OrganizationRankMemberDtoAssembler organizationRankMemberDtoAssembler;

    @Transactional(readOnly = true)
    public Optional<UserRankCurrentDto> findCurrentRank(long userId) {
        return userRankRepository
                .findByUserIdAndEndDateIsNull(userId)
                .flatMap(row -> toCurrentDto(row));
    }

    @Transactional(readOnly = true)
    public UserRankSelfDto getSelfRank(long userId) {
        Optional<UserRank> open = userRankRepository.findByUserIdAndEndDateIsNull(userId);
        List<UserRankHistoryItemDto> history =
                userRankRepository.findByUserIdOrderByStartDateDescIdDesc(userId).stream()
                        .map(row -> toHistoryItem(row))
                        .toList();
        return UserRankSelfDto.builder()
                .current(open.flatMap(row -> toCurrentDto(row)).orElse(null))
                .history(history)
                .build();
    }

    @Transactional(readOnly = true)
    public OrganizationRankMemberPageDto listOrganizationMembers(
            long organizationId, ListQuery query) {
        Pageable pageable = OrganizationRankMemberListDefinition.INSTANCE.pageable(query);
        Specification<UserOrganizationMembership> spec =
                Specification.allOf(
                        OrganizationRankMemberListSpecs.activeInOrganization(organizationId),
                        OrganizationRankMemberListDefinition.INSTANCE.searchSpecification(query));

        Page<UserOrganizationMembership> entityPage = membershipRepository.findAll(spec, pageable);
        List<Long> ids =
                entityPage.getContent().stream().map(UserOrganizationMembership::getId).toList();
        List<UserOrganizationMembership> hydrated =
                ids.isEmpty() ? List.of() : membershipRepository.findWithUserByIdIn(ids);
        List<UserOrganizationMembership> ordered =
                PageReorder.reorderByIds(ids, hydrated, UserOrganizationMembership::getId);

        List<Long> userIds = ordered.stream().map(m -> m.getUser().getId()).distinct().toList();
        Map<Long, UserRank> openByUser =
                userIds.isEmpty()
                        ? Map.of()
                        : userRankRepository.findByUserIdInAndEndDateIsNull(userIds).stream()
                                .collect(
                                        Collectors.toMap(UserRank::getUserId, r -> r, (a, b) -> a));
        Map<Long, TenantRank> ranksById = loadRanks(openByUser.values());

        List<OrganizationRankMemberDto> content =
                ordered.stream()
                        .map(m -> toOrganizationRankMemberDto(m, openByUser, ranksById))
                        .toList();
        Page<OrganizationRankMemberDto> result =
                new PageImpl<>(content, entityPage.getPageable(), entityPage.getTotalElements());
        return organizationRankMemberDtoAssembler.toPageDto(result);
    }

    private OrganizationRankMemberDto toOrganizationRankMemberDto(
            UserOrganizationMembership membership,
            Map<Long, UserRank> openByUser,
            Map<Long, TenantRank> ranksById) {
        UserRank open = openByUser.get(membership.getUser().getId());
        OrganizationRankMemberDto.OrganizationRankMemberDtoBuilder builder =
                OrganizationRankMemberDto.builder()
                        .userId(membership.getUser().getId())
                        .firstName(membership.getUser().getFirstName())
                        .lastName(membership.getUser().getLastName())
                        .mainOrganization(Boolean.TRUE.equals(membership.getMain()));
        if (open != null) {
            TenantRank rank = ranksById.get(open.getTenantRankId());
            if (rank != null) {
                builder.currentRank(toRankSummary(rank, open));
            }
        }
        return builder.build();
    }

    @Transactional
    public UserRankCurrentDto assignFromOrganization(
            long organizationId, long userId, SetUserRankRequest request) {
        requireMainOrganization(organizationId, userId);
        return assignRank(userId, request, false);
    }

    @Transactional
    public UserRankCurrentDto assignFirstRank(long userId, SetUserRankRequest request) {
        if (userRankRepository.findByUserIdAndEndDateIsNull(userId).isPresent()) {
            throw new ConflictException("User already has a rank");
        }
        return assignRank(userId, request, true);
    }

    @Transactional
    public UserRankCurrentDto assignFromOnboarding(
            long userId, long tenantRankId, LocalDate startDate, String reason) {
        SetUserRankRequest request =
                SetUserRankRequest.builder()
                        .tenantRankId(tenantRankId)
                        .startDate(startDate)
                        .reason(reason)
                        .build();
        return assignFirstRank(userId, request);
    }

    @Transactional
    public UserRankCurrentDto assignFromWorkflow(
            long userId, long tenantRankId, LocalDate startDate, String reason) {
        SetUserRankRequest request =
                SetUserRankRequest.builder()
                        .tenantRankId(tenantRankId)
                        .startDate(startDate)
                        .reason(reason)
                        .build();
        return assignFirstRank(userId, request);
    }

    @Transactional(readOnly = true)
    public boolean hasOpenRank(long userId) {
        return userRankRepository.findByUserIdAndEndDateIsNull(userId).isPresent();
    }

    private UserRankCurrentDto assignRank(
            long userId, SetUserRankRequest request, boolean firstRankOnly) {
        long tenantId = TenantScopeGuard.requireActiveTenantId();
        TenantRank rank = requireActiveRank(request.getTenantRankId());
        validateRequest(request);
        Optional<UserRank> open = userRankRepository.findByUserIdAndEndDateIsNull(userId);
        if (firstRankOnly && open.isPresent()) {
            throw new ConflictException("User already has a rank");
        }
        if (open.isPresent()) {
            UserRank current = open.get();
            LocalDate endDate = request.getStartDate().minusDays(1);
            if (endDate.isBefore(current.getStartDate())) {
                throw new ConflictException("Start date must be after current rank start date");
            }
            current.setEndDate(endDate);
            userRankRepository.saveAndFlush(current);
        }
        String reason =
                StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null;
        UserRank saved =
                userRankRepository.save(
                        UserRank.builder()
                                .tenantId(tenantId)
                                .userId(userId)
                                .tenantRankId(rank.getId())
                                .startDate(request.getStartDate())
                                .reason(reason)
                                .build());
        return toCurrentDto(saved)
                .orElseThrow(() -> new IllegalStateException("Saved rank row missing"));
    }

    private void requireMainOrganization(long organizationId, long userId) {
        Instant now = Instant.now();
        UserOrganizationMembership main =
                membershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, now)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "User has no active main organization"));
        if (!main.getOrganization().getId().equals(organizationId)) {
            throw new AccessDeniedException(
                    "Rank assignment is only allowed for the user's main organization");
        }
    }

    private TenantRank requireActiveRank(long tenantRankId) {
        TenantRank rank =
                tenantRankRepository
                        .findById(tenantRankId)
                        .orElseThrow(() -> new ConflictException("Unknown rank"));
        if (!rank.isActive() || rank.isArchived()) {
            throw new ConflictException("Rank is not active");
        }
        return rank;
    }

    private static void validateRequest(SetUserRankRequest request) {
        if (request.getStartDate() == null) {
            throw new ConflictException("startDate is required");
        }
    }

    private Map<Long, TenantRank> loadRanks(Collection<UserRank> rows) {
        Set<Long> ids = rows.stream().map(UserRank::getTenantRankId).collect(Collectors.toSet());
        Map<Long, TenantRank> map = new HashMap<>();
        for (Long id : ids) {
            tenantRankRepository.findById(id).ifPresent(r -> map.put(id, r));
        }
        return map;
    }

    private Optional<UserRankCurrentDto> toCurrentDto(UserRank row) {
        return tenantRankRepository
                .findById(row.getTenantRankId())
                .map(
                        rank ->
                                UserRankCurrentDto.builder()
                                        .tenantRankId(rank.getId())
                                        .configKey(rank.getConfigKey())
                                        .name(rank.getName())
                                        .abbreviation(rank.getAbbreviation())
                                        .startDate(row.getStartDate())
                                        .reason(row.getReason())
                                        .build());
    }

    private UserRankHistoryItemDto toHistoryItem(UserRank row) {
        TenantRank rank = tenantRankRepository.findById(row.getTenantRankId()).orElse(null);
        return UserRankHistoryItemDto.builder()
                .tenantRankId(row.getTenantRankId())
                .configKey(rank != null ? rank.getConfigKey() : null)
                .name(rank != null ? rank.getName() : null)
                .abbreviation(rank != null ? rank.getAbbreviation() : null)
                .startDate(row.getStartDate())
                .endDate(row.getEndDate())
                .reason(row.getReason())
                .build();
    }

    private UserRankSummaryDto toRankSummary(TenantRank rank, UserRank row) {
        return UserRankSummaryDto.builder()
                .tenantRankId(rank.getId())
                .configKey(rank.getConfigKey())
                .name(rank.getName())
                .abbreviation(rank.getAbbreviation())
                .sortOrder(rank.getSortOrder())
                .startDate(row.getStartDate())
                .build();
    }
}
