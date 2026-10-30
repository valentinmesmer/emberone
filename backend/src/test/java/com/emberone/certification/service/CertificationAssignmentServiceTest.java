/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.certification.domain.UserCertificationAssignment;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CertificationAssignmentServiceTest {

    private static final long USER_ID = 5L;
    private static final long TENANT_ID = 1L;
    private static final long ORG_ID = 10L;

    @Mock UserCertificationAssignmentRepository assignmentRepository;
    @Mock TenantCertDefinitionRepository tenantDefinitionRepository;
    @Mock CertificationCatalogService catalogService;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock TokenService tokenService;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock ApplicationEventPublisher eventPublisher;

    private CertificationAssignmentService service;

    @BeforeEach
    void setUp() {
        service =
                new CertificationAssignmentService(
                        assignmentRepository,
                        tenantDefinitionRepository,
                        catalogService,
                        membershipRepository,
                        tokenService,
                        organizationAccessService,
                        eventPublisher);
    }

    @Test
    void isDefinitionAssignedToUser_directAssignment() {
        when(assignmentRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                UserCertificationAssignment.builder()
                                        .tenantCertDefinitionId(100L)
                                        .startDate(LocalDate.now().minusDays(1))
                                        .build()));
        when(tenantDefinitionRepository.findById(100L))
                .thenReturn(
                        Optional.of(
                                TenantCertDefinition.builder()
                                        .id(100L)
                                        .active(true)
                                        .archived(false)
                                        .build()));

        assertThat(service.isDefinitionAssignedToUser(USER_ID, 100L)).isTrue();
        assertThat(service.isDefinitionAssignedToUser(USER_ID, 200L)).isFalse();
    }

    @Test
    void isDefinitionAssignedToUser_ignoresArchivedDefinition() {
        when(assignmentRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                UserCertificationAssignment.builder()
                                        .tenantCertDefinitionId(100L)
                                        .startDate(LocalDate.now().minusDays(1))
                                        .build()));
        when(tenantDefinitionRepository.findById(100L))
                .thenReturn(
                        Optional.of(
                                TenantCertDefinition.builder()
                                        .id(100L)
                                        .active(false)
                                        .archived(true)
                                        .build()));

        assertThat(service.isDefinitionAssignedToUser(USER_ID, 100L)).isFalse();
    }

    @Test
    void isDefinitionAssignedToUser_ignoresInactiveAssignments() {
        when(assignmentRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                UserCertificationAssignment.builder()
                                        .tenantCertDefinitionId(100L)
                                        .endDate(LocalDate.now().minusDays(1))
                                        .build()));

        assertThat(service.isDefinitionAssignedToUser(USER_ID, 100L)).isFalse();
    }

    @Test
    void pageAssignmentsReturnsEmptyPageWhenNoRows() {
        when(assignmentRepository.findByUserIdOrderByIdDesc(USER_ID, PageRequest.of(0, 50)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        var page = service.pageAssignments(USER_ID, 0, 50);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}
