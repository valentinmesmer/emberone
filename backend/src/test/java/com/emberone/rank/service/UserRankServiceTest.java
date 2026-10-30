/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.platform.exception.AccessDeniedException;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.rank.domain.UserRank;
import com.emberone.rank.dto.OrganizationRankMemberPageDto;
import com.emberone.rank.dto.SetUserRankRequest;
import com.emberone.rank.mapper.OrganizationRankMemberDtoAssembler;
import com.emberone.rank.repository.UserRankRepository;
import com.emberone.rank.tenant.TenantRank;
import com.emberone.rank.tenant.TenantRankRepository;
import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserRankServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 10L;
    private static final long ORG_ID = 100L;

    @Mock UserRankRepository userRankRepository;
    @Mock TenantRankRepository tenantRankRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock OrganizationRankMemberDtoAssembler organizationRankMemberDtoAssembler;

    @InjectMocks UserRankService userRankService;

    @BeforeEach
    void bindTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void assignFirstRankRejectsWhenOpenRankExists() {
        when(userRankRepository.findByUserIdAndEndDateIsNull(USER_ID))
                .thenReturn(Optional.of(UserRank.builder().id(1L).build()));

        assertThatThrownBy(
                        () ->
                                userRankService.assignFirstRank(
                                        USER_ID,
                                        SetUserRankRequest.builder()
                                                .tenantRankId(5L)
                                                .startDate(LocalDate.of(2026, 1, 1))
                                                .reason("Entry")
                                                .build()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void assignFromOrganizationRequiresMainOrganization() {
        Organization otherOrg = Organization.builder().id(999L).build();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(any(), any()))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(otherOrg)
                                        .main(true)
                                        .build()));

        assertThatThrownBy(
                        () ->
                                userRankService.assignFromOrganization(
                                        ORG_ID,
                                        USER_ID,
                                        SetUserRankRequest.builder()
                                                .tenantRankId(5L)
                                                .startDate(LocalDate.of(2026, 2, 1))
                                                .reason("Promotion")
                                                .build()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void promotionClosesOpenRowAndCreatesNewOne() {
        Organization mainOrg = Organization.builder().id(ORG_ID).build();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(any(), any()))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .main(true)
                                        .build()));
        UserRank open =
                UserRank.builder()
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .tenantRankId(1L)
                        .startDate(LocalDate.of(2025, 1, 1))
                        .reason("Entry")
                        .build();
        when(userRankRepository.findByUserIdAndEndDateIsNull(USER_ID))
                .thenReturn(Optional.of(open));
        when(tenantRankRepository.findById(2L))
                .thenReturn(
                        Optional.of(
                                TenantRank.builder()
                                        .id(2L)
                                        .configKey("BM")
                                        .name("Brandmeister")
                                        .abbreviation("BM")
                                        .active(true)
                                        .archived(false)
                                        .build()));
        when(userRankRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRankRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            UserRank rank = invocation.getArgument(0);
                            if (rank.getId() == null && rank.getEndDate() == null) {
                                rank.setId(99L);
                            }
                            return rank;
                        });
        userRankService.assignFromOrganization(
                ORG_ID,
                USER_ID,
                SetUserRankRequest.builder()
                        .tenantRankId(2L)
                        .startDate(LocalDate.of(2026, 3, 1))
                        .reason("Promotion")
                        .build());

        assertThat(open.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        ArgumentCaptor<UserRank> captor = ArgumentCaptor.forClass(UserRank.class);
        verify(userRankRepository).saveAndFlush(open);
        verify(userRankRepository).save(captor.capture());
        UserRank saved = captor.getAllValues().getLast();
        assertThat(saved.getTenantRankId()).isEqualTo(2L);
        assertThat(saved.getEndDate()).isNull();
    }

    @Test
    void listOrganizationMembersIncludesUsersWithoutRank() {
        User user = User.builder().id(USER_ID).firstName("Alex").lastName("Muster").build();
        UserOrganizationMembership membership =
                UserOrganizationMembership.builder().id(1L).user(user).main(true).build();
        when(membershipRepository.findAll(
                        org.mockito.ArgumentMatchers
                                .<Specification<UserOrganizationMembership>>any(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership)));
        when(membershipRepository.findWithUserByIdIn(List.of(1L))).thenReturn(List.of(membership));
        when(userRankRepository.findByUserIdInAndEndDateIsNull(List.of(USER_ID)))
                .thenReturn(List.of());
        when(organizationRankMemberDtoAssembler.toPageDto(any()))
                .thenAnswer(
                        invocation -> {
                            var page =
                                    invocation.getArgument(
                                            0, org.springframework.data.domain.Page.class);
                            return OrganizationRankMemberPageDto.builder()
                                    .content(page.getContent())
                                    .build();
                        });
        var page =
                userRankService.listOrganizationMembers(ORG_ID, new ListQuery(0, null, null, null));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getCurrentRank()).isNull();
    }
}
