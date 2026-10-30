/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.DeactivateOrganizationRequest;
import com.emberone.organization.exception.OrganizationDeactivatedException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.domain.User;
import com.emberone.user.service.UserDeactivationService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrganizationDeactivationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock UserOrganizationMembershipService membershipService;
    @Mock UserDeactivationService userDeactivationService;

    @InjectMocks OrganizationDeactivationService service;

    @Test
    void deactivateRejectsMismatchedConfirmationName() {
        Organization root = Organization.builder().id(1L).tenantId(7L).name("Feuerwehr").build();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(root));

        DeactivateOrganizationRequest request = new DeactivateOrganizationRequest();
        request.setConfirmationName("Other");

        assertThatThrownBy(() -> service.deactivateOrganization(7L, 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Confirmation");
    }

    @Test
    void deactivateDeactivatesMainOrgUsersAndEndsSecondaryMemberships() {
        Organization root = Organization.builder().id(1L).tenantId(7L).name("Feuerwehr").build();
        Organization child =
                Organization.builder()
                        .id(2L)
                        .tenantId(7L)
                        .name("Child")
                        .parentOrganization(root)
                        .build();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(root));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(root, child));

        User mainUser = User.builder().id(10L).tenantId(7L).build();
        User secondaryUser = User.builder().id(20L).tenantId(7L).build();
        UserOrganizationMembership mainMembership =
                UserOrganizationMembership.builder()
                        .id(100L)
                        .main(true)
                        .user(mainUser)
                        .organization(root)
                        .build();
        UserOrganizationMembership secondaryMembership =
                UserOrganizationMembership.builder()
                        .id(200L)
                        .main(false)
                        .user(secondaryUser)
                        .organization(child)
                        .build();
        when(membershipRepository.findActiveByOrganization_IdIn(any(), any(Instant.class)))
                .thenReturn(List.of(mainMembership, secondaryMembership));

        DeactivateOrganizationRequest request = new DeactivateOrganizationRequest();
        request.setConfirmationName("Feuerwehr");

        service.deactivateOrganization(7L, 1L, request);

        verify(userDeactivationService).deactivateUser(10L);
        verify(membershipService)
                .endActiveMembershipForUserDeactivation(
                        eq(secondaryMembership), any(Instant.class));
        verify(organizationRepository)
                .saveAll(
                        argThat(
                                (Iterable<Organization> saved) -> {
                                    int count = 0;
                                    for (Organization ignored : saved) {
                                        count++;
                                    }
                                    return count == 2;
                                }));
    }

    @Test
    void deactivateRejectsAlreadyDeactivatedOrganization() {
        Organization root =
                Organization.builder()
                        .id(1L)
                        .tenantId(7L)
                        .name("Feuerwehr")
                        .deactivatedAt(Instant.now())
                        .build();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(root));

        DeactivateOrganizationRequest request = new DeactivateOrganizationRequest();
        request.setConfirmationName("Feuerwehr");

        assertThatThrownBy(() -> service.deactivateOrganization(7L, 1L, request))
                .isInstanceOf(OrganizationDeactivatedException.class);
    }
}
