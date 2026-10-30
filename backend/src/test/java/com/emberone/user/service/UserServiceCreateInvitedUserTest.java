/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.identity.dto.ProvisionIdentityUserRequest;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.dto.CreateUserRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.event.UserCreatedEvent;
import com.emberone.user.mapper.UserMapper;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Unit test for the admin invite path: user, contact, identity, invitation - no membership. */
@ExtendWith(MockitoExtension.class)
class UserServiceCreateInvitedUserTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper mapper;
    @Mock IdentityUserService identityUserService;
    @Mock OrganizationRepository organizationRepository;
    @Mock UsernameService usernameService;
    @Mock UserPersonalNumberService userPersonalNumberService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock OnboardingInvitationService onboardingInvitationService;
    @Mock TokenService tokenService;
    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;

    @InjectMocks UserService service;

    @Test
    void adminInviteCreatesUserIdentityContactAndOpenInvitationWithoutMembership() {
        long tenantId = 7L;
        long organizationId = 10L;
        long invitingUserId = 42L;

        Organization organization =
                Organization.builder().id(organizationId).tenantId(tenantId).build();
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setPrivateEmail("ada@example.com");

        when(organizationRepository.findById(organizationId))
                .thenReturn(java.util.Optional.of(organization));
        when(userPersonalNumberService.allocateNext(tenantId)).thenReturn(3L);
        when(usernameService.generateUniqueUsername(tenantId, "Ada", "Lovelace"))
                .thenReturn("ada.lovelace.fw");
        when(tokenService.getReferencedUserId()).thenReturn(invitingUserId);
        when(userRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(1L);
                            return user;
                        });
        when(mapper.toDto(any())).thenReturn(UserDto.builder().id(1L).build());
        when(onboardingInvitationService.recordInvitationForAdmission(
                        tenantId, 1L, organizationId, invitingUserId))
                .thenReturn(false);

        service.createInvitedUser(tenantId, organizationId, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getTenantId()).isEqualTo(tenantId);
        assertThat(savedUser.getUsername()).isEqualTo("ada.lovelace.fw");
        UserContactInformation contact = savedUser.getContactInformation();
        assertThat(contact).isNotNull();
        assertThat(contact.getPrivateEmail()).isEqualTo("ada@example.com");
        assertThat(savedUser.getOrganizationMemberships()).isEmpty();

        ArgumentCaptor<ProvisionIdentityUserRequest> provisionCaptor =
                ArgumentCaptor.forClass(ProvisionIdentityUserRequest.class);
        verify(identityUserService).provisionUser(provisionCaptor.capture());
        assertThat(provisionCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(provisionCaptor.getValue().getUsername()).isEqualTo("ada.lovelace.fw");

        verify(onboardingInvitationService)
                .recordInvitationForAdmission(tenantId, 1L, organizationId, invitingUserId);

        verify(organizationMembershipRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(UserCreatedEvent.class));
    }
}
