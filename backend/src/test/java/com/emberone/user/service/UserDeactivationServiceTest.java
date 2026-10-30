/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.identity.service.IdentityUserService;
import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import com.emberone.user.event.UserDeactivatedEvent;
import com.emberone.user.exception.UserAlreadyDeactivatedException;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserDeactivationServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 7L;
    private static final Long ORG_ID = 10L;

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;
    @Mock UserOrganizationMembershipService organizationMembershipService;
    @Mock UserOrganizationGroupMembershipService groupMembershipService;
    @Mock IdentityUserService identityUserService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks UserDeactivationService service;

    @Test
    void deactivateUserEndsMembershipsDeprovisionsIdentityAndPublishesEvent() {
        User user = activeUser();
        UserOrganizationMembership membership = activeMembership(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizationMembershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(List.of(membership));

        service.deactivateUser(USER_ID);

        verify(groupMembershipService)
                .endAllActiveMembershipsForUserInOrganization(eq(USER_ID), eq(ORG_ID), any());
        verify(organizationMembershipService)
                .endActiveMembershipForUserDeactivation(eq(membership), any());
        verify(userRepository).save(user);
        assertThat(user.getDeactivatedAt()).isNotNull();
        verify(identityUserService).scheduleDeprovision(TENANT_ID, USER_ID);

        ArgumentCaptor<UserDeactivatedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserDeactivatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
    }

    @Test
    void deactivateUserThrowsWhenAlreadyDeactivated() {
        User user = activeUser();
        user.setDeactivatedAt(Instant.now());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.deactivateUser(USER_ID))
                .isInstanceOf(UserAlreadyDeactivatedException.class);

        verify(organizationMembershipRepository, never()).findActiveByUser_Id(any(), any());
    }

    @Test
    void deactivateUserThrowsWhenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateUser(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static User activeUser() {
        return User.builder()
                .id(USER_ID)
                .tenantId(TENANT_ID)
                .personalNumber(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .username("ada")
                .build();
    }

    private static UserOrganizationMembership activeMembership(User user) {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        return UserOrganizationMembership.builder()
                .id(100L)
                .tenantId(TENANT_ID)
                .user(user)
                .organization(organization)
                .main(true)
                .startDate(Instant.now().minusSeconds(3600))
                .build();
    }
}
