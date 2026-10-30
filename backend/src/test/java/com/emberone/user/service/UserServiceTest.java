/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.identity.dto.ProvisionIdentityUserRequest;
import com.emberone.identity.dto.UpdateIdentityUserRequest;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.dto.OrganizationTreeDto;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.dto.OrganizationGroupJoinTargetDto;
import com.emberone.organizationgroup.dto.OrganizationGroupLeaveTargetDto;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TokenService;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.dto.*;
import com.emberone.user.event.NotificationEmailChangeRequestedEvent;
import com.emberone.user.event.UserCreatedEvent;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.mapper.UserMapper;
import com.emberone.user.repository.UserContactInformationRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.CreateUserOrganizationMembershipRequest;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import com.emberone.workflow.service.WorkflowOpenConcurrencyService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper mapper;
    @Mock IdentityUserService identityUserService;
    @Mock OrganizationGroupService organizationGroupService;
    @Mock AllMembersGroupService allMembersGroupService;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock UserOrganizationGroupMembershipService groupMembershipService;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserOrganizationMembershipMapper organizationMembershipMapper;
    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;
    @Mock UserOrganizationMembershipService organizationMembershipService;
    @Mock UserContactInformationRepository userContactInformationRepository;
    @Mock UsernameService usernameService;
    @Mock UserPersonalNumberService userPersonalNumberService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock WorkflowTypeMappingRepository workflowTypeMappingRepository;
    @Mock WorkflowOpenConcurrencyService workflowOpenConcurrencyService;
    @Mock OrganizationMapper organizationMapper;
    @Mock OnboardingInvitationRepository onboardingInvitationRepository;
    @Mock OnboardingInvitationService onboardingInvitationService;
    @Mock TokenService tokenService;

    @InjectMocks UserService service;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    private void stubPersonalNumberAllocation() {
        when(userPersonalNumberService.allocateNext(any())).thenReturn(3L);
    }

    @Test
    void createInitialTenantMemberProvisionsUserAndClearsTenantContext() {
        stubPersonalNumberAllocation();
        SetupUserRequest request = setupRequest();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        UserDto dto = UserDto.builder().id(1L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(usernameService.generateUniqueUsername(anyLong(), eq("Ada"), eq("Lovelace")))
                .thenReturn("ada.lovelace.fw");
        when(userRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(1L);
                            return user;
                        });
        when(mapper.toDto(any())).thenReturn(dto);

        assertThat(service.createInitialTenantMember(7L, request)).isSameAs(dto);

        verify(eventPublisher).publishEvent(new UserCreatedEvent(7L, 1L, 10L));
        verify(identityUserService).provisionUser(any());
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void createInitialTenantMemberRejectsOrganizationFromDifferentTenant() {
        SetupUserRequest request = setupRequest();
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(99L).build()));

        assertThatThrownBy(() -> service.createInitialTenantMember(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organization does not belong to tenant");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void createInitialTenantMemberThrowsWhenOrganizationMissing() {
        SetupUserRequest request = setupRequest();
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createInitialTenantMember(7L, request))
                .isInstanceOf(OrganizationNotFoundException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void getUserByIdReturnsMappedDto() {
        User user = User.builder().id(1L).tenantId(7L).build();
        UserDto dto = UserDto.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mapper.toDto(user)).thenReturn(dto);

        assertThat(service.getUserById(1L)).isSameAs(dto);
    }

    @Test
    void getUserByIdThrowsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateNotificationEmailRequestsVerificationForNewAddress() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .contactInformation(
                                UserContactInformation.builder()
                                        .privateEmail("old@example.com")
                                        .build())
                        .build();
        user.getContactInformation().setUser(user);
        UpdateContactInformationRequest request = new UpdateContactInformationRequest();
        request.setPrivateEmail("new@example.com");
        UserDto dto = UserDto.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(mapper.toDto(user)).thenReturn(dto);

        assertThat(service.updateContactInformationSelf(1L, request)).isSameAs(dto);

        assertThat(user.getContactInformation().getPrivateEmail()).isEqualTo("old@example.com");
        assertThat(user.getContactInformation().getPendingEmail()).isEqualTo("new@example.com");
        assertThat(user.getContactInformation().getEmailVerificationTokenHash()).isNotBlank();
        assertThat(user.getContactInformation().getEmailVerificationExpiresAt()).isNotNull();
        ArgumentCaptor<NotificationEmailChangeRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationEmailChangeRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().pendingEmail()).isEqualTo("new@example.com");
        assertThat(eventCaptor.getValue().rawToken()).isNotBlank();
    }

    @Test
    void updateNotificationEmailClearsPendingWhenSameAddress() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .contactInformation(
                                UserContactInformation.builder()
                                        .privateEmail("ada@example.com")
                                        .pendingEmail("other@example.com")
                                        .emailVerificationTokenHash("abc")
                                        .build())
                        .build();
        UpdateContactInformationRequest request = new UpdateContactInformationRequest();
        request.setPrivateEmail(" Ada@Example.com ");
        UserDto dto = UserDto.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(mapper.toDto(user)).thenReturn(dto);

        assertThat(service.updateContactInformationSelf(1L, request)).isSameAs(dto);

        assertThat(user.getContactInformation().getPendingEmail()).isNull();
        assertThat(user.getContactInformation().getEmailVerificationTokenHash()).isNull();
        verify(eventPublisher, never())
                .publishEvent(any(NotificationEmailChangeRequestedEvent.class));
    }

    @Test
    void verifyNotificationEmailAppliesPendingAndSyncsIdentity() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .username("ada")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .contactInformation(
                                UserContactInformation.builder()
                                        .privateEmail("old@example.com")
                                        .build())
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(mapper.toDto(user)).thenReturn(UserDto.builder().id(1L).build());

        UpdateContactInformationRequest change = new UpdateContactInformationRequest();
        change.setPrivateEmail("new@example.com");
        service.updateContactInformationSelf(1L, change);
        ArgumentCaptor<NotificationEmailChangeRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationEmailChangeRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        String rawToken = eventCaptor.getValue().rawToken();

        UserDto result = service.verifyNotificationEmailSelf(1L, rawToken);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(user.getContactInformation().getPrivateEmail()).isEqualTo("new@example.com");
        assertThat(user.getContactInformation().getPendingEmail()).isNull();
        ArgumentCaptor<UpdateIdentityUserRequest> identityCaptor =
                ArgumentCaptor.forClass(UpdateIdentityUserRequest.class);
        verify(identityUserService).updateUser(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getPrivateEmail()).isEqualTo("new@example.com");
    }

    @Test
    void verifyNotificationEmailByTokenAppliesPendingWhenTenantBound() {
        TenantContextHolder.setTenantId(7L);
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .username("ada")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .contactInformation(
                                UserContactInformation.builder()
                                        .privateEmail("old@example.com")
                                        .build())
                        .build();
        user.getContactInformation().setUser(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(mapper.toDto(user)).thenReturn(UserDto.builder().id(1L).build());

        UpdateContactInformationRequest change = new UpdateContactInformationRequest();
        change.setPrivateEmail("new@example.com");
        service.updateContactInformationSelf(1L, change);
        ArgumentCaptor<NotificationEmailChangeRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationEmailChangeRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        String rawToken = eventCaptor.getValue().rawToken();

        when(userContactInformationRepository.findByEmailVerificationTokenHash(any()))
                .thenReturn(Optional.of(user.getContactInformation()));

        service.verifyNotificationEmailByToken(rawToken);

        assertThat(user.getContactInformation().getPrivateEmail()).isEqualTo("new@example.com");
        assertThat(user.getContactInformation().getPendingEmail()).isNull();
        verify(identityUserService).updateUser(any(UpdateIdentityUserRequest.class));
    }

    @Test
    void applyApprovedNameChangeUpdatesUserAndSyncsIdentity() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .firstName("Old")
                        .lastName("Name")
                        .contactInformation(
                                UserContactInformation.builder()
                                        .privateEmail("ada@example.com")
                                        .build())
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(usernameService.generateUniqueUsername(anyLong(), eq("Ada"), eq("Lovelace")))
                .thenReturn("ada.lovelace.fw");
        when(userRepository.save(user)).thenReturn(user);

        service.applyApprovedNameChange(1L, 7L, " Ada ", " Lovelace ");

        assertThat(user.getFirstName()).isEqualTo("Ada");
        assertThat(user.getLastName()).isEqualTo("Lovelace");
        assertThat(user.getUsername()).isEqualTo("ada.lovelace.fw");
        ArgumentCaptor<UpdateIdentityUserRequest> captor =
                ArgumentCaptor.forClass(UpdateIdentityUserRequest.class);
        verify(identityUserService).updateUser(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("ada.lovelace.fw");
    }

    @Test
    void applyApprovedNameChangeRejectsCrossTenantUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(8L).build()));

        assertThatThrownBy(() -> service.applyApprovedNameChange(1L, 7L, "Ada", "Lovelace"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the tenant");
    }

    @Test
    void applyApprovedAddressChangeUpdatesContactInformation() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .contactInformation(UserContactInformation.builder().build())
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.applyApprovedAddressChange(1L, 7L, " Main 1 ", " Berlin ", " 10115 ", " DE ");

        assertThat(user.getContactInformation().getStreet()).isEqualTo("Main 1");
        assertThat(user.getContactInformation().getCity()).isEqualTo("Berlin");
        verify(userRepository).save(user);
    }

    @Test
    void applyApprovedAddressChangeRequiresExistingContactInformation() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));

        assertThatThrownBy(
                        () ->
                                service.applyApprovedAddressChange(
                                        1L, 7L, "Main", "Berlin", "10115", "DE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no contact information");
    }

    @Test
    void createInvitedUserBuildsUserContactAndInvitationWithoutMembership() {
        stubPersonalNumberAllocation();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        CreateUserRequest request = request();
        UserDto dto = UserDto.builder().id(1L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(usernameService.generateUniqueUsername(anyLong(), eq("Ada"), eq("Lovelace")))
                .thenReturn("ada.lovelace.fw");
        when(tokenService.getReferencedUserId()).thenReturn(99L);
        when(userRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(1L);
                            return user;
                        });
        when(mapper.toDto(any())).thenReturn(dto);
        when(onboardingInvitationService.recordInvitationForAdmission(7L, 1L, 10L, 99L))
                .thenReturn(false);

        assertThat(service.createInvitedUser(7L, 10L, request)).isSameAs(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getTenantId()).isEqualTo(7L);
        assertThat(user.getPersonalNumber()).isEqualTo(3L);
        assertThat(user.getUsername()).isEqualTo("ada.lovelace.fw");
        assertThat(user.getContactInformation().getPrivateEmail()).isEqualTo("ada@example.com");
        assertThat(user.getOrganizationMemberships()).isEmpty();

        verify(onboardingInvitationService).recordInvitationForAdmission(7L, 1L, 10L, 99L);
        verify(organizationMembershipService, never()).createMembership(any());

        verify(eventPublisher, never()).publishEvent(any(UserCreatedEvent.class));
        ArgumentCaptor<ProvisionIdentityUserRequest> provisionCaptor =
                ArgumentCaptor.forClass(ProvisionIdentityUserRequest.class);
        verify(identityUserService).provisionUser(provisionCaptor.capture());
        assertThat(provisionCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(provisionCaptor.getValue().getUsername()).isEqualTo("ada.lovelace.fw");
    }

    @Test
    void createInvitedUserDirectlyAdmitsWhenNoOnboardingStepsConfigured() {
        stubPersonalNumberAllocation();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        CreateUserRequest request = request();
        UserDto dto = UserDto.builder().id(1L).build();
        User savedUser =
                User.builder().id(1L).tenantId(7L).organizationMemberships(List.of()).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(usernameService.generateUniqueUsername(anyLong(), eq("Ada"), eq("Lovelace")))
                .thenReturn("ada.lovelace.fw");
        when(tokenService.getReferencedUserId()).thenReturn(99L);
        when(userRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(1L);
                            return user;
                        });
        when(mapper.toDto(any())).thenReturn(dto);
        when(onboardingInvitationService.recordInvitationForAdmission(7L, 1L, 10L, 99L))
                .thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));

        assertThat(service.createInvitedUser(7L, 10L, request)).isSameAs(dto);

        verify(onboardingInvitationService).recordInvitationForAdmission(7L, 1L, 10L, 99L);
    }

    @Test
    void createInvitedUserRejectsOrganizationFromDifferentTenant() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(8L).build()));

        assertThatThrownBy(() -> service.createInvitedUser(7L, 10L, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organization does not belong to tenant");
    }

    @Test
    void ensureMembershipRejectsCrossTenantUserAndOrganization() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(8L).build()));
        assertThatThrownBy(
                        () -> service.ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(8L).build()));
        assertThatThrownBy(
                        () -> service.ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organization");
        verify(eventPublisher, never()).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void ensureMembershipCreatesMainMembershipOnlyWhenUserHasNone() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));

        service.ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 10L);

        ArgumentCaptor<CreateUserOrganizationMembershipRequest> captor =
                ArgumentCaptor.forClass(CreateUserOrganizationMembershipRequest.class);
        verify(organizationMembershipService).createMembership(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(10L);
        assertThat(captor.getValue().isMain()).isTrue();
        verify(allMembersGroupService, never()).ensureUserInAllMembersGroup(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void ensureMembershipIsNotMainWhenUserAlreadyHasAnActiveMainOrganization() {
        Organization currentMain = Organization.builder().id(9L).tenantId(7L).build();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        User user = User.builder().id(1L).tenantId(7L).build();
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .user(user)
                                .organization(currentMain)
                                .main(true)
                                .startDate(Instant.now())
                                .build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));

        service.ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 10L);

        ArgumentCaptor<CreateUserOrganizationMembershipRequest> captor =
                ArgumentCaptor.forClass(CreateUserOrganizationMembershipRequest.class);
        verify(organizationMembershipService).createMembership(captor.capture());
        assertThat(captor.getValue().isMain()).isFalse();
        verify(allMembersGroupService, never()).ensureUserInAllMembersGroup(any(), any());
    }

    @Test
    void updateNotificationEmailRequiresExistingEmail() {
        User user = User.builder().id(1L).tenantId(7L).build();
        UpdateContactInformationRequest request = new UpdateContactInformationRequest();
        request.setPrivateEmail("new@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateContactInformationSelf(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("notification email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRejectsUserOutsideOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateUserData(1L, 10L, new UpdateUserDataRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
        verify(mapper, never()).updateFromRequest(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRegeneratesUsernameAndSynchronizesIdentity() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .firstName("Old")
                        .lastName("Name")
                        .contactInformation(
                                UserContactInformation.builder()
                                        .privateEmail("ada@example.com")
                                        .build())
                        .build();
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .user(user)
                                .organization(organization)
                                .main(true)
                                .startDate(Instant.now())
                                .build());
        UpdateUserDataRequest request = new UpdateUserDataRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(true);
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            user.setFirstName("Ada");
                            user.setLastName("Lovelace");
                            return null;
                        })
                .when(mapper)
                .updateFromRequest(request, user);
        when(usernameService.generateUniqueUsername(anyLong(), eq("Ada"), eq("Lovelace")))
                .thenReturn("ada.lovelace.fw");
        when(userRepository.save(user)).thenReturn(user);

        service.updateUserData(1L, 10L, request);

        assertThat(user.getUsername()).isEqualTo("ada.lovelace.fw");
        ArgumentCaptor<UpdateIdentityUserRequest> captor =
                ArgumentCaptor.forClass(UpdateIdentityUserRequest.class);
        verify(identityUserService).updateUser(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("ada.lovelace.fw");
        assertThat(captor.getValue().getPrivateEmail()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
    }

    @Test
    void missingUserFailsWithoutSideEffects() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.updateContactInformationSelf(
                                        99L, new UpdateContactInformationRequest()))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrganizationTreeBuildsHierarchyWithActiveGroupsPerOrganization() {
        Organization parent =
                Organization.builder().id(10L).name("Parent").organizationCode("P").build();
        Organization child =
                Organization.builder()
                        .id(11L)
                        .name("Child")
                        .organizationCode("C")
                        .parentOrganization(parent)
                        .build();
        User user = User.builder().id(1L).tenantId(7L).build();
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .user(user)
                                .organization(parent)
                                .main(true)
                                .startDate(Instant.now())
                                .build());
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .user(user)
                                .organization(child)
                                .main(false)
                                .startDate(Instant.now())
                                .build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationGroupService.getGroupsForUserInOrganization(1L, 10L, false))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(20L)
                                        .organizationId(10L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));

        List<OrganizationTreeDto> tree = service.getOrganizationTreeForUser(1L);

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().getId()).isEqualTo(10L);
        assertThat(tree.getFirst().getChildren())
                .singleElement()
                .satisfies(childNode -> assertThat(childNode.getId()).isEqualTo(11L));
        assertThat(tree.getFirst().getGroups())
                .singleElement()
                .satisfies(group -> assertThat(group.getName()).isEqualTo("Crew"));
    }

    @Test
    void getUsersByOrganizationIdFiltersGroupMembershipsToOrganizationGroups() {
        Organization organization = Organization.builder().id(10L).name("Parent").build();
        User user = User.builder().id(1L).lastName("Lovelace").firstName("Ada").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationGroupRepository.findIdsByOrganizationIdAndTypeIn(
                        eq(10L), eq(OrganizationGroupType.userInterfaceVisibleTypes())))
                .thenReturn(List.of(20L));
        when(organizationMembershipRepository.findActiveByOrganization_Id(
                        eq(10L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationMembership.builder()
                                        .user(user)
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));
        UserDto dto =
                UserDto.builder()
                        .id(1L)
                        .lastName("Lovelace")
                        .firstName("Ada")
                        .organizationGroupMemberships(
                                List.of(
                                        UserOrganizationGroupMembershipDto.builder()
                                                .organizationGroupId(20L)
                                                .build(),
                                        UserOrganizationGroupMembershipDto.builder()
                                                .organizationGroupId(99L)
                                                .build()))
                        .build();
        when(mapper.toDto(user)).thenReturn(dto);

        assertThat(service.getUsersByOrganizationId(10L))
                .singleElement()
                .satisfies(
                        result ->
                                assertThat(result.getOrganizationGroupMemberships())
                                        .extracting(
                                                UserOrganizationGroupMembershipDto
                                                        ::getOrganizationGroupId)
                                        .containsExactly(20L));
    }

    @Test
    void addUserToLeadershipGroupCreatesLeaderMembership() {
        when(organizationGroupService.getGroupsByOrganizationId(10L))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(20L)
                                        .name("Führung")
                                        .type(OrganizationGroupType.LEADERSHIP)
                                        .build(),
                                OrganizationGroupDto.builder()
                                        .id(21L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));

        service.addUserToLeadershipGroupAsLeader(1L, 10L);

        ArgumentCaptor<CreateUserOrganizationGroupMembershipRequest> captor =
                ArgumentCaptor.forClass(CreateUserOrganizationGroupMembershipRequest.class);
        verify(groupMembershipService).createSystemManagedMembership(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getOrganizationGroupId()).isEqualTo(20L);
        assertThat(captor.getValue().getMembershipType()).isEqualTo(GroupMembershipType.LEADER);
        assertThat(captor.getValue().getStartDate()).isBeforeOrEqualTo(Instant.now());
        assertThat(captor.getValue().getStartDate()).isAfter(Instant.now().minusSeconds(5));
    }

    @Test
    void addUserToLeadershipGroupFailsWhenLeadershipGroupMissing() {
        when(organizationGroupService.getGroupsByOrganizationId(10L))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(21L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));

        assertThatThrownBy(() -> service.addUserToLeadershipGroupAsLeader(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Leadership group not found");
        verify(groupMembershipService, never()).createSystemManagedMembership(any());
    }

    @Test
    void assertUserBelongsToOrganizationUsesActiveMembershipQuery() {
        TenantContextHolder.setTenantId(7L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(organizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(true);

        service.assertUserBelongsToOrganization(1L, 10L);

        verify(organizationMembershipRepository)
                .existsActiveByUser_IdAndOrganization_Id(eq(1L), eq(10L), any(Instant.class));
    }

    @Test
    void assertUserBelongsToOrganizationRejectsInactiveMembership() {
        TenantContextHolder.setTenantId(7L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));

        assertThatThrownBy(() -> service.assertUserBelongsToOrganization(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("specified organization");
    }

    @Test
    void assertUserBelongsToOrganizationRejectsDifferentTenantBeforeMembershipQuery() {
        TenantContextHolder.setTenantId(7L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(8L).build()));

        assertThatThrownBy(() -> service.assertUserBelongsToOrganization(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant");

        verify(organizationMembershipRepository, never())
                .existsActiveByUser_IdAndOrganization_Id(any(), any(), any());
    }

    @Test
    void ensureMembershipSkipsSaveWhenUserAlreadyBelongsToOrganization() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        User user = User.builder().id(1L).tenantId(7L).build();
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .user(user)
                                .organization(organization)
                                .main(true)
                                .startDate(Instant.now())
                                .build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));

        service.ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 10L);

        verify(organizationMembershipService, never()).createMembership(any());
        verify(allMembersGroupService, never()).ensureUserInAllMembersGroup(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrganizationMembershipsSortsMainFirstThenOrganizationName() {
        Organization alpha = Organization.builder().id(10L).name("Alpha").build();
        Organization zulu = Organization.builder().id(11L).name("Zulu").build();
        User user = User.builder().id(1L).build();
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .id(2L)
                                .user(user)
                                .organization(zulu)
                                .main(false)
                                .startDate(Instant.now())
                                .build());
        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .id(1L)
                                .user(user)
                                .organization(alpha)
                                .main(true)
                                .startDate(Instant.now())
                                .build());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationMembershipMapper.toDto(user.getOrganizationMemberships().get(1)))
                .thenReturn(
                        UserOrganizationMembershipDto.builder()
                                .id(1L)
                                .main(true)
                                .organization(
                                        OrganizationDto.builder().id(10L).name("Alpha").build())
                                .build());
        when(organizationMembershipMapper.toDto(user.getOrganizationMemberships().get(0)))
                .thenReturn(
                        UserOrganizationMembershipDto.builder()
                                .id(2L)
                                .main(false)
                                .organization(
                                        OrganizationDto.builder().id(11L).name("Zulu").build())
                                .build());

        assertThat(service.getOrganizationMembershipsForUser(1L))
                .extracting(UserOrganizationMembershipDto::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void getOrganizationLeaveTargetsExcludesOrganizationsWhereUserIsInLeadership() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization stay = Organization.builder().id(10L).tenantId(7L).name("Stay").build();
        Organization leave = Organization.builder().id(11L).tenantId(7L).name("Leave").build();
        UserOrganizationMembership stayMembership =
                UserOrganizationMembership.builder().organization(stay).build();
        UserOrganizationMembership leaveMembership =
                UserOrganizationMembership.builder().organization(leave).build();
        long workflowTypeId = WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(10L)
                                        .enabled(true)
                                        .build(),
                                WorkflowDefinition.builder()
                                        .organizationId(11L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        10L, workflowTypeId))
                .thenReturn(true);
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        11L, workflowTypeId))
                .thenReturn(true);
        when(organizationMembershipRepository.findActiveByUser_Id(eq(1L), any(Instant.class)))
                .thenReturn(List.of(stayMembership, leaveMembership));
        when(allMembersGroupService.hasActiveLeadershipMembership(1L, 10L)).thenReturn(true);
        when(allMembersGroupService.hasActiveLeadershipMembership(1L, 11L)).thenReturn(false);
        when(organizationMapper.toDto(leave))
                .thenReturn(OrganizationDto.builder().id(11L).name("Leave").build());

        assertThat(service.getOrganizationLeaveTargetsForUser(1L))
                .extracting(OrganizationDto::getId)
                .containsExactly(11L);
    }

    @Test
    void getOrganizationLeaveTargetsExcludesOrganizationsWithOpenLeaveRequest() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization openLeave =
                Organization.builder().id(10L).tenantId(7L).name("Open leave").build();
        Organization available =
                Organization.builder().id(11L).tenantId(7L).name("Available").build();
        UserOrganizationMembership openLeaveMembership =
                UserOrganizationMembership.builder().organization(openLeave).build();
        UserOrganizationMembership availableMembership =
                UserOrganizationMembership.builder().organization(available).build();
        long workflowTypeId = WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(10L)
                                        .enabled(true)
                                        .build(),
                                WorkflowDefinition.builder()
                                        .organizationId(11L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        10L, workflowTypeId))
                .thenReturn(true);
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        11L, workflowTypeId))
                .thenReturn(true);
        when(organizationMembershipRepository.findActiveByUser_Id(eq(1L), any(Instant.class)))
                .thenReturn(List.of(openLeaveMembership, availableMembership));
        when(allMembersGroupService.hasActiveLeadershipMembership(1L, 11L)).thenReturn(false);
        when(workflowOpenConcurrencyService.findOpenKeys(1L, workflowTypeId))
                .thenReturn(java.util.Set.of("org:10"));
        when(organizationMapper.toDto(available))
                .thenReturn(OrganizationDto.builder().id(11L).name("Available").build());

        assertThat(service.getOrganizationLeaveTargetsForUser(1L))
                .extracting(OrganizationDto::getId)
                .containsExactly(11L);
    }

    @Test
    void getOrganizationGroupLeaveTargetsExcludesLeadershipGroups() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha").build();
        long workflowTypeId = WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(10L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        10L, workflowTypeId))
                .thenReturn(true);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationGroupService.getGroupsForUserInOrganization(1L, 10L, false))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(50L)
                                        .name("Führung")
                                        .type(OrganizationGroupType.LEADERSHIP)
                                        .build(),
                                OrganizationGroupDto.builder()
                                        .id(51L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));

        List<OrganizationGroupLeaveTargetDto> targets =
                service.getOrganizationGroupLeaveTargetsForUser(1L);
        assertThat(targets)
                .extracting(OrganizationGroupLeaveTargetDto::getOrganizationGroupId)
                .containsExactly(51L);
        assertThat(targets.getFirst().getOrganizationGroupType())
                .isEqualTo(OrganizationGroupType.CUSTOM);
    }

    @Test
    void getOrganizationGroupLeaveTargetsExcludesGroupsWithOpenLeaveRequest() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha").build();
        long workflowTypeId = WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(10L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        10L, workflowTypeId))
                .thenReturn(true);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationGroupService.getGroupsForUserInOrganization(1L, 10L, false))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(50L)
                                        .name("Open leave")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build(),
                                OrganizationGroupDto.builder()
                                        .id(51L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));
        when(workflowOpenConcurrencyService.findOpenKeys(1L, workflowTypeId))
                .thenReturn(java.util.Set.of("group:50"));

        List<OrganizationGroupLeaveTargetDto> targets =
                service.getOrganizationGroupLeaveTargetsForUser(1L);
        assertThat(targets)
                .extracting(OrganizationGroupLeaveTargetDto::getOrganizationGroupId)
                .containsExactly(51L);
    }

    @Test
    void getOrganizationGroupJoinTargetsExcludesLeadershipAndExistingMemberships() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha").build();
        UserOrganizationMembership membership =
                UserOrganizationMembership.builder().organization(organization).build();
        long workflowTypeId = WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST.getId();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(10L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        10L, workflowTypeId))
                .thenReturn(true);
        when(organizationMembershipRepository.findActiveByUser_Id(eq(1L), any(Instant.class)))
                .thenReturn(List.of(membership));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationGroupService.getGroupsForUserInOrganization(1L, 10L, false))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(50L)
                                        .name("Already in")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));
        when(organizationGroupService.getAssignableGroupsByOrganizationId(10L))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(50L)
                                        .name("Already in")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build(),
                                OrganizationGroupDto.builder()
                                        .id(51L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build(),
                                OrganizationGroupDto.builder()
                                        .id(52L)
                                        .name("Führung")
                                        .type(OrganizationGroupType.LEADERSHIP)
                                        .build()));
        when(workflowOpenConcurrencyService.findOpenKeys(1L, workflowTypeId))
                .thenReturn(java.util.Set.of());

        List<OrganizationGroupJoinTargetDto> targets =
                service.getOrganizationGroupJoinTargetsForUser(1L);
        assertThat(targets)
                .extracting(OrganizationGroupJoinTargetDto::getOrganizationGroupId)
                .containsExactly(51L);
        assertThat(targets.getFirst().getOrganizationGroupType())
                .isEqualTo(OrganizationGroupType.CUSTOM);
    }

    @Test
    void getOrganizationGroupJoinTargetsExcludesGroupsWithOpenJoinRequest() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha").build();
        UserOrganizationMembership membership =
                UserOrganizationMembership.builder().organization(organization).build();
        long workflowTypeId = WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST.getId();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(10L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        10L, workflowTypeId))
                .thenReturn(true);
        when(organizationMembershipRepository.findActiveByUser_Id(eq(1L), any(Instant.class)))
                .thenReturn(List.of(membership));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationGroupService.getGroupsForUserInOrganization(1L, 10L, false))
                .thenReturn(List.of());
        when(organizationGroupService.getAssignableGroupsByOrganizationId(10L))
                .thenReturn(
                        List.of(
                                OrganizationGroupDto.builder()
                                        .id(51L)
                                        .name("Crew")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build(),
                                OrganizationGroupDto.builder()
                                        .id(52L)
                                        .name("Rescue")
                                        .type(OrganizationGroupType.CUSTOM)
                                        .build()));
        when(workflowOpenConcurrencyService.findOpenKeys(1L, workflowTypeId))
                .thenReturn(java.util.Set.of("group:51"));

        assertThat(service.getOrganizationGroupJoinTargetsForUser(1L))
                .extracting(OrganizationGroupJoinTargetDto::getOrganizationGroupId)
                .containsExactly(52L);
    }

    @Test
    void getJoinableOrganizationsExcludesOrganizationsWithOpenJoinRequest() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .organizationMemberships(new java.util.ArrayList<>())
                        .build();
        Organization mainOrg = Organization.builder().id(10L).tenantId(7L).name("Main").build();
        Organization joinableOrg =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        long workflowTypeId = WorkflowType.ORGANIZATION_JOIN_REQUEST.getId();

        user.getOrganizationMemberships()
                .add(
                        UserOrganizationMembership.builder()
                                .organization(mainOrg)
                                .main(true)
                                .startDate(Instant.now())
                                .build());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationMembershipRepository.findActiveByUser_IdAndMainTrue(
                        eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .main(true)
                                        .startDate(Instant.now())
                                        .build()));
        when(workflowOpenConcurrencyService.findOpenKeys(1L, workflowTypeId))
                .thenReturn(java.util.Set.of("org:30"));
        when(workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .organizationId(20L)
                                        .enabled(true)
                                        .build(),
                                WorkflowDefinition.builder()
                                        .organizationId(30L)
                                        .enabled(true)
                                        .build()));
        when(workflowTypeMappingRepository.existsByOrganizationIdAndWorkflowTypeId(
                        20L, workflowTypeId))
                .thenReturn(true);
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(joinableOrg));
        when(organizationMapper.toDto(joinableOrg))
                .thenReturn(OrganizationDto.builder().id(20L).name("Target").build());

        assertThat(service.getJoinableOrganizationsForUser(1L))
                .extracting(OrganizationDto::getId)
                .containsExactly(20L);
    }

    private SetupUserRequest setupRequest() {
        SetupUserRequest request = new SetupUserRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setPrivateEmail("ada@example.com");
        request.setStreet("Main 1");
        request.setCity("Berlin");
        request.setPostalCode("10115");
        request.setCountry("DE");
        request.setMainOrganizationId(10L);
        return request;
    }

    private CreateUserRequest request() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setPrivateEmail("ada@example.com");
        request.setStreet("Main 1");
        request.setCity("Berlin");
        request.setPostalCode("10115");
        request.setCountry("DE");
        return request;
    }
}
