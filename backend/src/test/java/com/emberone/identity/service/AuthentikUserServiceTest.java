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

import com.emberone.identity.client.AuthentikApiClient;
import com.emberone.identity.client.dto.*;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.exception.IdentityException;
import com.emberone.identity.provider.IdpUserCreationResult;
import com.emberone.platform.security.EmberOneAppRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthentikUserServiceTest {

    @Mock AuthentikApiClient client;
    private AuthentikUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthentikUserService(client);
        ReflectionTestUtils.setField(service, "apiToken", "secret");
    }

    @Test
    void mergeRoleMembershipKeepsUnrelatedGroupsAndReplacesEmberOneRole() {
        Map<String, String> roleGroups =
                Map.of(
                        "role:USER", "user-pk",
                        "role:TENANT_ADMIN", "tenant-pk",
                        "role:GLOBAL_ADMIN", "global-pk");

        assertThat(
                        AuthentikUserService.mergeEmberOneRoleMembership(
                                List.of("other", "user-pk", "tenant-pk"),
                                "GLOBAL_ADMIN",
                                roleGroups))
                .containsExactly("other", "global-pk");
    }

    @Test
    void createUserAssignsNormalizedRoleGroupAndTemporaryPassword() {
        mockRoleGroups();
        when(client.createUser(any()))
                .thenReturn(AuthentikUserResponse.builder().pk("remote-1").build());

        IdpUserCreationResult result = service.createUser(identity(), 7L, "tenant_admin");

        assertThat(result.idpUserId()).isEqualTo("remote-1");
        assertThat(result.temporaryPassword())
                .hasSize(16)
                .matches(".*[A-Z].*")
                .matches(".*[a-z].*")
                .matches(".*[0-9].*");
        ArgumentCaptor<AuthentikUserRequest> captor =
                ArgumentCaptor.forClass(AuthentikUserRequest.class);
        verify(client).createUser(captor.capture());
        assertThat(captor.getValue().getGroups()).containsExactly("TENANT_ADMIN-pk");
        assertThat(captor.getValue().getAttributes().get("emberone_tenant_id"))
                .containsExactly("7");
        assertThat(captor.getValue().getEmail()).isEqualTo("ada@example.com");
        verify(client).setPassword(org.mockito.ArgumentMatchers.eq("remote-1"), any());
    }

    @Test
    void updatePreservesUnrelatedGroupsAndReplacesOldRole() {
        mockRoleGroups();
        when(client.getUser("remote-1"))
                .thenReturn(
                        new AuthentikUserDetailResponse(
                                List.of("external", "USER-pk", "TENANT_ADMIN-pk")));
        IdentityUser identity = identity();
        identity.setIdpUserId("remote-1");

        service.updateUser(identity, 7L, EmberOneAppRole.GLOBAL_ADMIN.getRoleName());

        ArgumentCaptor<AuthentikUserRequest> captor =
                ArgumentCaptor.forClass(AuthentikUserRequest.class);
        verify(client).updateUser(org.mockito.ArgumentMatchers.eq("remote-1"), captor.capture());
        assertThat(captor.getValue().getGroups()).containsExactly("external", "GLOBAL_ADMIN-pk");
        assertThat(captor.getValue().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void missingTokenStopsRemoteMutation() {
        ReflectionTestUtils.setField(service, "apiToken", " ");

        assertThatThrownBy(() -> service.createUser(identity(), 7L, "USER"))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("token");
        verify(client, never()).createUser(any());
    }

    @Test
    void remoteFailuresAreWrappedAsIdentityException() {
        mockRoleGroups();
        when(client.createUser(any())).thenThrow(new RuntimeException("remote down"));

        assertThatThrownBy(() -> service.createUser(identity(), 7L, "USER"))
                .isInstanceOf(IdentityException.class)
                .hasMessage("Failed to create Authentik user")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void mergeRoleMembershipKeepsTargetWhenAlreadyPresent() {
        Map<String, String> roleGroups =
                Map.of(
                        "role:USER", "user-pk",
                        "role:TENANT_ADMIN", "tenant-pk",
                        "role:GLOBAL_ADMIN", "global-pk");

        assertThat(
                        AuthentikUserService.mergeEmberOneRoleMembership(
                                List.of("other", "global-pk"), "GLOBAL_ADMIN", roleGroups))
                .containsExactly("other", "global-pk");
    }

    @Test
    void mergeRoleMembershipIgnoresBlankGroupPks() {
        Map<String, String> roleGroups = Map.of("role:USER", "user-pk");
        List<String> currentGroups = new java.util.ArrayList<>();
        currentGroups.add(" ");
        currentGroups.add(null);
        currentGroups.add("custom");

        assertThat(
                        AuthentikUserService.mergeEmberOneRoleMembership(
                                currentGroups, "USER", roleGroups))
                .containsExactly("custom", "user-pk");
    }

    @Test
    void updateUserNoOpsWhenAuthentikIdMissing() {
        IdentityUser identity = identity();

        service.updateUser(identity, 7L, "USER");

        verify(client, never()).getUser(any());
        verify(client, never()).updateUser(any(), any());
    }

    @Test
    void createUserRejectsEmptyRemoteResponse() {
        mockRoleGroups();
        when(client.createUser(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createUser(identity(), 7L, "USER"))
                .isInstanceOf(IdentityException.class)
                .hasMessage("Authentik returned empty user response");
    }

    @Test
    void createUserRejectsMissingRoleGroupMapping() {
        mockRoleGroups();
        when(client.listGroupsByExactName("role:USER"))
                .thenReturn(new AuthentikGroupListResponse(List.of()));

        assertThatThrownBy(() -> service.createUser(identity(), 7L, "USER"))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("role:USER");
    }

    @Test
    void deleteUserCallsAuthentikApi() {
        service.deleteUser("remote-1");
        verify(client).deleteUser("remote-1");
    }

    @Test
    void deleteUserSkipsBlankId() {
        service.deleteUser("  ");
        verify(client, never()).deleteUser(any());
    }

    private void mockRoleGroups() {
        for (EmberOneAppRole role : EmberOneAppRole.values()) {
            String name = "role:" + role.getRoleName();
            when(client.listGroupsByExactName(name))
                    .thenReturn(
                            new AuthentikGroupListResponse(
                                    List.of(
                                            new AuthentikGroupRow(
                                                    role.getRoleName() + "-pk", name))));
        }
    }

    private IdentityUser identity() {
        return IdentityUser.builder()
                .emberoneUserId(1L)
                .username("ada")
                .displayName("Ada Lovelace")
                .givenName("Ada")
                .surname("Lovelace")
                .privateEmail("ada@example.com")
                .accountEnabled(true)
                .appRole("USER")
                .build();
    }
}
