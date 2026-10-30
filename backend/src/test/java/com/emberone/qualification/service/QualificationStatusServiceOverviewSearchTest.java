/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.qualification.dto.QualificationOverviewGroupDto;
import com.emberone.qualification.dto.QualificationOverviewMemberDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class QualificationStatusServiceOverviewSearchTest {

    private final List<QualificationOverviewGroupDto> groups =
            List.of(
                    group(1L, "Atemschutz", member(10L, "Anna Berg"), member(11L, "Max Roth")),
                    group(2L, "Maschinist", member(11L, "Max Roth")),
                    group(3L, "Funk"));

    @Test
    void blankSearch_returnsAllGroupsUnchanged() {
        assertThat(QualificationStatusService.filterGroupsBySearch(groups, null)).isEqualTo(groups);
        assertThat(QualificationStatusService.filterGroupsBySearch(groups, "  ")).isEqualTo(groups);
    }

    @Test
    void groupNameMatch_keepsAllMembers() {
        List<QualificationOverviewGroupDto> result =
                QualificationStatusService.filterGroupsBySearch(groups, "ATEMS");

        assertThat(result)
                .extracting(QualificationOverviewGroupDto::getName)
                .containsExactly("Atemschutz");
        assertThat(result.get(0).getMembers()).hasSize(2);
    }

    @Test
    void memberMatch_keepsOnlyMatchingMembers_andDropsEmptyGroups_inBackendOrder() {
        List<QualificationOverviewGroupDto> result =
                QualificationStatusService.filterGroupsBySearch(groups, "max");

        assertThat(result)
                .extracting(QualificationOverviewGroupDto::getName)
                .containsExactly("Atemschutz", "Maschinist");
        assertThat(result.get(0).getMembers())
                .extracting(QualificationOverviewMemberDto::getUserDisplayName)
                .containsExactly("Max Roth");
    }

    @Test
    void allTokensMustMatchSameText() {
        assertThat(QualificationStatusService.filterGroupsBySearch(groups, "anna berg")).hasSize(1);
        assertThat(QualificationStatusService.filterGroupsBySearch(groups, "anna roth")).isEmpty();
    }

    @Test
    void tooManyWords_isRejected() {
        assertThatThrownBy(
                        () ->
                                QualificationStatusService.filterGroupsBySearch(
                                        groups, "a b c d e f"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static QualificationOverviewGroupDto group(
            Long id, String name, QualificationOverviewMemberDto... members) {
        return QualificationOverviewGroupDto.builder()
                .tenantQualificationDefinitionId(id)
                .configKey("key-" + id)
                .name(name)
                .members(List.of(members))
                .appointableMembers(List.of())
                .build();
    }

    private static QualificationOverviewMemberDto member(Long id, String name) {
        return QualificationOverviewMemberDto.builder().userId(id).userDisplayName(name).build();
    }
}
