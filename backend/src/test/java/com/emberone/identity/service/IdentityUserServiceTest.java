/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.dto.ProvisionIdentityUserRequest;
import com.emberone.identity.dto.UpdateIdentityUserRequest;
import com.emberone.identity.event.UserWelcomeNeededEvent;
import com.emberone.identity.event.UsernameChangedEvent;
import com.emberone.identity.provider.IdentityProviderUserProvisioner;
import com.emberone.identity.provider.IdpUserCreationResult;
import com.emberone.identity.repository.IdentityUserRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class IdentityUserServiceTest {

    @Mock IdentityUserRepository repository;
    @Mock UserRepository userRepository;
    @Mock IdentityProviderUserProvisioner identityProviderUserProvisioner;
    @Mock ApplicationEventPublisher eventPublisher;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void provisionCreatesRemoteIdentityBeforeSavingAndSendsWelcomeNotification() {
        IdentityUserService service = service();
        when(identityProviderUserProvisioner.createUser(
                        any(),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("USER")))
                .thenReturn(new IdpUserCreationResult("remote-1", "Temp123Password"));

        service.provisionUser(provision("unknown-role"));

        ArgumentCaptor<IdentityUser> captor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIdpUserId()).isEqualTo("remote-1");
        assertThat(captor.getValue().getAppRole()).isEqualTo("USER");

        UserWelcomeNeededEvent event = captureEvent(UserWelcomeNeededEvent.class);
        assertThat(event.tenantId()).isEqualTo(7L);
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.privateEmail()).isEqualTo("ada@example.com");
        assertThat(event.firstName()).isEqualTo("Ada");
        assertThat(event.username()).isEqualTo("ada");
        assertThat(event.temporaryPassword()).isEqualTo("Temp123Password");
    }

    @Test
    void updateExistingIdentitySyncsRemoteAndNotifiesUsernameChange() {
        IdentityUserService service = service();
        IdentityUser identity = identity();
        when(repository.findByEmberoneUserId(1L)).thenReturn(Optional.of(identity));

        service.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .username("ada.new")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .privateEmail("ada@example.com")
                        .build());

        verify(identityProviderUserProvisioner).updateUser(identity, 7L, "USER");
        UsernameChangedEvent event = captureEvent(UsernameChangedEvent.class);
        assertThat(event.tenantId()).isEqualTo(7L);
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.previousUsername()).isEqualTo("ada");
        assertThat(event.newUsername()).isEqualTo("ada.new");
        verify(repository).save(identity);
    }

    @Test
    void updateExistingIdentityNotifiesUsernameChangeInAppWithoutEmail() {
        IdentityUserService service = service();
        IdentityUser identity = identity();
        when(repository.findByEmberoneUserId(1L)).thenReturn(Optional.of(identity));

        service.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .username("ada.new")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .build());

        captureEvent(UsernameChangedEvent.class);
    }

    @Test
    void updateRoleRejectsCrossTenantAndAlwaysClearsContext() {
        IdentityUserService service = service();
        when(repository.findByEmberoneUserId(1L))
                .thenReturn(
                        Optional.of(
                                IdentityUser.builder().emberoneUserId(1L).tenantId(8L).build()));

        assertThatThrownBy(() -> service.updateAppRole(7L, 1L, "GLOBAL_ADMIN"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(identityProviderUserProvisioner, never()).updateUser(any(), any(), any());
    }

    @Test
    void updateMissingIdentityFallsBackToProvision() {
        IdentityUserService service = service();
        when(repository.findByEmberoneUserId(1L)).thenReturn(Optional.empty());
        when(identityProviderUserProvisioner.createUser(
                        any(),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("USER")))
                .thenReturn(new IdpUserCreationResult("remote-2", "Temp123Password"));

        service.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .username("ada")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .privateEmail("ada@example.com")
                        .build());

        verify(repository).save(any(IdentityUser.class));
        captureEvent(UserWelcomeNeededEvent.class);
        verify(identityProviderUserProvisioner, never()).updateUser(any(), any(), any());
    }

    @Test
    void syncCreatesRemoteIdentityWhenAuthentikIdMissing() {
        IdentityUserService service = service();
        IdentityUser identity = identity();
        identity.setIdpUserId(null);
        when(repository.findByEmberoneUserId(1L)).thenReturn(Optional.of(identity));
        when(identityProviderUserProvisioner.createUser(
                        any(),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("USER")))
                .thenReturn(new IdpUserCreationResult("remote-2", "Temp123Password"));

        service.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .username("ada")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .privateEmail("ada@example.com")
                        .build());

        verify(identityProviderUserProvisioner).createUser(identity, 7L, "USER");
        captureEvent(UserWelcomeNeededEvent.class);
        verify(identityProviderUserProvisioner, never()).updateUser(any(), any(), any());
    }

    @Test
    void updateAppRoleNormalizesAndSyncsRemote() {
        IdentityUserService service = service();
        IdentityUser identity = identity();
        when(repository.findByEmberoneUserId(1L)).thenReturn(Optional.of(identity));

        service.updateAppRole(7L, 1L, " tenant_admin ");

        assertThat(identity.getAppRole()).isEqualTo("TENANT_ADMIN");
        verify(identityProviderUserProvisioner).updateUser(identity, 7L, "TENANT_ADMIN");
        verify(repository).save(identity);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void provisionNormalizesKnownAdminRoles() {
        IdentityUserService service = service();
        when(identityProviderUserProvisioner.createUser(
                        any(),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("GLOBAL_ADMIN")))
                .thenReturn(new IdpUserCreationResult("remote-1", "Temp123Password"));

        service.provisionUser(
                ProvisionIdentityUserRequest.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .username("admin")
                        .firstName("Ada")
                        .lastName("Admin")
                        .privateEmail("ada@example.com")
                        .role("global_admin")
                        .build());

        ArgumentCaptor<IdentityUser> captor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAppRole()).isEqualTo("GLOBAL_ADMIN");
    }

    @Test
    void updateUserSkipsWhenTargetDeactivated() {
        IdentityUserService service = service();
        when(userRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .id(1L)
                                        .tenantId(7L)
                                        .personalNumber(1L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .username("ada")
                                        .deactivatedAt(java.time.Instant.now())
                                        .build()));

        service.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(1L)
                        .tenantId(7L)
                        .username("ada.new")
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .build());

        verify(identityProviderUserProvisioner, never()).updateUser(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void deprovisionAllIdpUsersForTenantPurgeDeletesEachLinkedIdpAccount() {
        IdentityUserService service = service();
        IdentityUser withIdp = identity();
        IdentityUser withoutIdp = identity();
        withoutIdp.setIdpUserId(null);
        when(repository.findAllByTenantIdOrderByIdAsc(7L)).thenReturn(List.of(withIdp, withoutIdp));

        service.deprovisionAllIdpUsersForTenantPurge(7L);

        verify(identityProviderUserProvisioner).deleteUser("remote-1");
        verify(identityProviderUserProvisioner, times(1)).deleteUser(any());
    }

    @Test
    void scheduleDeprovisionMarksPendingWithoutIdpMutation() {
        IdentityUserService service = service();
        IdentityUser identity = identity();
        when(repository.findByEmberoneUserId(1L)).thenReturn(Optional.of(identity));

        service.scheduleDeprovision(7L, 1L);

        verify(identityProviderUserProvisioner, never()).deleteUser(any());
        verify(repository).save(identity);
        assertThat(identity.getAccountEnabled()).isFalse();
        assertThat(identity.getDeprovisionPendingAt()).isNotNull();
        assertThat(identity.getIdpUserId()).isEqualTo("remote-1");
    }

    private <T> T captureEvent(Class<T> type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(type);
        return type.cast(captor.getValue());
    }

    private IdentityUserService service() {
        return new IdentityUserService(
                repository, userRepository, identityProviderUserProvisioner, eventPublisher);
    }

    private ProvisionIdentityUserRequest provision(String role) {
        return ProvisionIdentityUserRequest.builder()
                .userId(1L)
                .tenantId(7L)
                .username("ada")
                .firstName("Ada")
                .lastName("Lovelace")
                .privateEmail("ada@example.com")
                .role(role)
                .build();
    }

    private IdentityUser identity() {
        return IdentityUser.builder()
                .tenantId(7L)
                .emberoneUserId(1L)
                .idpUserId("remote-1")
                .username("ada")
                .givenName("Ada")
                .surname("Lovelace")
                .displayName("Ada Lovelace")
                .privateEmail("ada@example.com")
                .accountEnabled(true)
                .appRole("USER")
                .build();
    }
}
