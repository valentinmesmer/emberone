/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.user.dto.GroupMembershipPeriodDto;
import com.emberone.user.dto.OrganizationMembershipPeriodDto;
import com.emberone.user.dto.SelfMembershipHistoryDto;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelfMembershipHistoryService {

    private final UserOrganizationMembershipRepository organizationMembershipRepository;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;

    /**
     * All started organization and group memberships of the user, active first, then by end and
     * start date descending. Groups that are hidden in the UI (e.g. all-members) are left out.
     */
    @Transactional(readOnly = true)
    public SelfMembershipHistoryDto getForUser(Long userId) {
        Instant now = Instant.now();
        return SelfMembershipHistoryDto.builder()
                .organizations(
                        organizationMembershipRepository.findByUser_Id(userId).stream()
                                .filter(m -> !m.getStartDate().isAfter(now))
                                .sorted(
                                        Comparator.comparing(
                                                        (UserOrganizationMembership m) ->
                                                                !(m.isActiveAt(now)
                                                                        && Boolean.TRUE.equals(
                                                                                m.getMain())))
                                                .thenComparing(m -> !m.isActiveAt(now))
                                                .thenComparing(
                                                        UserOrganizationMembership::getEndDate,
                                                        Comparator.nullsFirst(
                                                                Comparator.reverseOrder()))
                                                .thenComparing(
                                                        UserOrganizationMembership::getStartDate,
                                                        Comparator.reverseOrder()))
                                .map(
                                        m ->
                                                OrganizationMembershipPeriodDto.builder()
                                                        .organizationId(m.getOrganization().getId())
                                                        .organizationName(
                                                                m.getOrganization().getName())
                                                        .organizationCode(
                                                                m.getOrganization()
                                                                        .getOrganizationCode())
                                                        .main(Boolean.TRUE.equals(m.getMain()))
                                                        .startDate(m.getStartDate())
                                                        .endDate(m.getEndDate())
                                                        .active(m.isActiveAt(now))
                                                        .build())
                                .toList())
                .groups(
                        groupMembershipRepository.findByUser_Id(userId).stream()
                                .filter(m -> !m.getStartDate().isAfter(now))
                                .filter(
                                        m ->
                                                m.getOrganizationGroup()
                                                        .getType()
                                                        .isVisibleInUserInterface())
                                .sorted(
                                        Comparator.comparing(
                                                        (UserOrganizationGroupMembership m) ->
                                                                !m.isActiveAt(now))
                                                .thenComparing(
                                                        UserOrganizationGroupMembership::getEndDate,
                                                        Comparator.nullsFirst(
                                                                Comparator.reverseOrder()))
                                                .thenComparing(
                                                        UserOrganizationGroupMembership
                                                                ::getStartDate,
                                                        Comparator.reverseOrder()))
                                .map(
                                        m -> {
                                            OrganizationGroup group = m.getOrganizationGroup();
                                            return GroupMembershipPeriodDto.builder()
                                                    .organizationId(group.getOrganization().getId())
                                                    .organizationGroupId(group.getId())
                                                    .organizationGroupName(group.getName())
                                                    .membershipType(m.getMembershipType())
                                                    .startDate(m.getStartDate())
                                                    .endDate(m.getEndDate())
                                                    .active(m.isActiveAt(now))
                                                    .build();
                                        })
                                .toList())
                .build();
    }
}
