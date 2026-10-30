/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.dutyfunction.tenant.TenantFunction;
import com.emberone.dutyfunction.tenant.TenantFunctionRepository;
import com.emberone.federalstate.baseline.FunctionGrantBaseline;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.standard.OrgGroupTemplate;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DutyFunctionStandardDomainTest {

    @Mock FunctionGrantRepository grantRepository;
    @Mock FunctionSelectionRepository selectionRepository;
    @Mock TenantFunctionRepository functionRepository;
    @Mock OrgGroupTemplateRepository orgGroupTemplateRepository;

    @InjectMocks DutyFunctionStandardDomain domain;

    @Test
    void replaceGrantsKeepsSavedRows() {
        Standard standard = Standard.builder().id(5L).tenantId(1L).name("SYSTEM").build();
        when(functionRepository.findByConfigKey("KDT"))
                .thenReturn(
                        Optional.of(
                                TenantFunction.builder()
                                        .configKey("KDT")
                                        .name("Kommandant")
                                        .abbreviation("Kdt")
                                        .active(true)
                                        .archived(false)
                                        .build()));
        when(selectionRepository.existsByStandardIdAndFunctionConfigKey(5L, "KDT"))
                .thenReturn(true);
        when(orgGroupTemplateRepository.findByStandardIdAndConfigKey(5L, "LEADERSHIP"))
                .thenReturn(
                        Optional.of(
                                OrgGroupTemplate.builder()
                                        .configKey("LEADERSHIP")
                                        .groupType(OrganizationGroupType.LEADERSHIP)
                                        .build()));
        when(grantRepository.findByStandardId(5L))
                .thenReturn(
                        List.of(
                                FunctionGrant.builder()
                                        .id(99L)
                                        .tenantId(1L)
                                        .standardId(5L)
                                        .organizationGroupConfigKey("LEADERSHIP")
                                        .membershipType(GroupMembershipType.LEADER)
                                        .functionConfigKey("KDT")
                                        .build()));
        when(grantRepository.findByStandardIdAndOrganizationGroupConfigKeyAndMembershipType(
                        5L, "LEADERSHIP", GroupMembershipType.LEADER))
                .thenReturn(
                        Optional.of(
                                FunctionGrant.builder()
                                        .id(99L)
                                        .tenantId(1L)
                                        .standardId(5L)
                                        .organizationGroupConfigKey("LEADERSHIP")
                                        .membershipType(GroupMembershipType.LEADER)
                                        .build()));

        domain.replaceGrants(
                standard,
                List.of(
                        new FunctionGrantBaseline(
                                "LEADERSHIP", GroupMembershipType.LEADER, "KDT")));

        verify(grantRepository, never()).delete(any());
        ArgumentCaptor<FunctionGrant> saved = ArgumentCaptor.forClass(FunctionGrant.class);
        verify(grantRepository).save(saved.capture());
        assertThat(saved.getValue().getFunctionConfigKey()).isEqualTo("KDT");
    }

    @Test
    void replaceGrantsRejectsMissingMembershipType() {
        Standard standard = Standard.builder().id(5L).tenantId(1L).name("SYSTEM").build();

        assertThatThrownBy(
                        () ->
                                domain.replaceGrants(
                                        standard,
                                        List.of(
                                                new FunctionGrantBaseline(
                                                        "LEADERSHIP", null, "KDT"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("membership type is required");
    }

    @Test
    void replaceGrantsRejectsAllMembersGroup() {
        Standard standard = Standard.builder().id(5L).tenantId(1L).name("SYSTEM").build();
        when(orgGroupTemplateRepository.findByStandardIdAndConfigKey(5L, "ALL_MEMBERS"))
                .thenReturn(
                        Optional.of(
                                OrgGroupTemplate.builder()
                                        .configKey("ALL_MEMBERS")
                                        .groupType(OrganizationGroupType.ALL_MEMBERS)
                                        .build()));

        assertThatThrownBy(
                        () ->
                                domain.replaceGrants(
                                        standard,
                                        List.of(
                                                new FunctionGrantBaseline(
                                                        "ALL_MEMBERS",
                                                        GroupMembershipType.MEMBER,
                                                        "KDT"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not allowed");
    }
}
