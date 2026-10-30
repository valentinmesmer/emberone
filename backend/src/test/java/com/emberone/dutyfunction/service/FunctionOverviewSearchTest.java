/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.dutyfunction.dto.OrganizationFunctionOverviewGroupDto;
import com.emberone.dutyfunction.dto.OrganizationFunctionOverviewMemberDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class FunctionOverviewSearchTest {

    private static final List<OrganizationFunctionOverviewGroupDto> GROUPS =
            List.of(
                    group(
                            "g1",
                            "Kommandant",
                            "Führung",
                            member("Anna", "Maier"),
                            member("Ben", "Huber")),
                    group("g2", "Gruppenführer", "Mannschaft", member("Carl", "Maier")));

    @Test
    void blankSearchKeepsEverything() {
        assertThat(DutyFunctionResolutionService.filterBySearch(GROUPS, null)).isEqualTo(GROUPS);
        assertThat(DutyFunctionResolutionService.filterBySearch(GROUPS, "  ")).isEqualTo(GROUPS);
    }

    @Test
    void groupLabelMatchKeepsAllMembersCaseInsensitive() {
        var result = DutyFunctionResolutionService.filterBySearch(GROUPS, "KOMMAND führung");
        assertThat(result)
                .extracting(OrganizationFunctionOverviewGroupDto::getGroupKey)
                .containsExactly("g1");
        assertThat(result.getFirst().getMembers()).hasSize(2);
    }

    @Test
    void memberMatchKeepsOnlyMatchingMembersAndDropsEmptyGroups() {
        var result = DutyFunctionResolutionService.filterBySearch(GROUPS, "maier");
        assertThat(result)
                .extracting(OrganizationFunctionOverviewGroupDto::getGroupKey)
                .containsExactly("g1", "g2");
        assertThat(result.getFirst().getMembers())
                .extracting(OrganizationFunctionOverviewMemberDto::getFirstName)
                .containsExactly("Anna");

        assertThat(DutyFunctionResolutionService.filterBySearch(GROUPS, "huber ben"))
                .extracting(OrganizationFunctionOverviewGroupDto::getGroupKey)
                .containsExactly("g1");
        assertThat(DutyFunctionResolutionService.filterBySearch(GROUPS, "nomatch")).isEmpty();
    }

    @Test
    void overlongSearchIsRejected() {
        assertThatThrownBy(
                        () -> DutyFunctionResolutionService.filterBySearch(GROUPS, "a".repeat(500)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OrganizationFunctionOverviewGroupDto group(
            String key,
            String function,
            String orgGroup,
            OrganizationFunctionOverviewMemberDto... members) {
        return OrganizationFunctionOverviewGroupDto.builder()
                .groupKey(key)
                .functionName(function)
                .organizationGroupName(orgGroup)
                .members(List.of(members))
                .build();
    }

    private static OrganizationFunctionOverviewMemberDto member(String first, String last) {
        return OrganizationFunctionOverviewMemberDto.builder()
                .firstName(first)
                .lastName(last)
                .build();
    }
}
