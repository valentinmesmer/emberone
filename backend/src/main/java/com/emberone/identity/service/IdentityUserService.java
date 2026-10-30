/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.dto.ProvisionIdentityUserRequest;
import com.emberone.identity.dto.UpdateIdentityUserRequest;
import com.emberone.identity.event.UserWelcomeNeededEvent;
import com.emberone.identity.event.UsernameChangedEvent;
import com.emberone.identity.provider.IdentityProviderUserProvisioner;
import com.emberone.identity.provider.IdpUserCreationResult;
import com.emberone.identity.repository.IdentityUserRepository;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUserService {

    private final IdentityUserRepository repository;
    private final UserRepository userRepository;
    private final IdentityProviderUserProvisioner identityProviderUserProvisioner;
    private final ApplicationEventPublisher eventPublisher;

    private static String normalizeAppRole(String role) {
        if (role == null || role.isBlank()) {
            return EmberOneAppRole.USER.getRoleName();
        }
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "GLOBAL_ADMIN", "TENANT_ADMIN", "USER" -> r;
            default -> EmberOneAppRole.USER.getRoleName();
        };
    }

    private static boolean usernameChanged(String previous, String next) {
        return !Objects.equals(
                        previous != null ? previous.trim() : "", next != null ? next.trim() : "")
                && StringUtils.hasText(next);
    }

    @Transactional
    public void provisionUser(ProvisionIdentityUserRequest request) {
        IdentityUser identityUser =
                IdentityUser.builder()
                        .tenantId(request.getTenantId())
                        .emberoneUserId(request.getUserId())
                        .displayName(
                                buildDisplayName(request.getFirstName(), request.getLastName()))
                        .givenName(request.getFirstName())
                        .surname(request.getLastName())
                        .username(request.getUsername())
                        .privateEmail(request.getPrivateEmail())
                        .accountEnabled(true)
                        .appRole(normalizeAppRole(request.getRole()))
                        .build();

        IdpUserCreationResult created =
                identityProviderUserProvisioner.createUser(
                        identityUser, request.getTenantId(), identityUser.getAppRole());
        identityUser.setIdpUserId(created.idpUserId());
        repository.save(identityUser);

        sendWelcomeNotification(
                request.getTenantId(),
                request.getUserId(),
                request.getPrivateEmail(),
                request.getFirstName(),
                request.getUsername(),
                created.temporaryPassword());
        log.info("Provisioned identity for user {}", request.getUserId());
    }

    @Transactional
    public void updateUser(UpdateIdentityUserRequest request) {
        if (isDeactivatedEmberOneUser(request.getUserId())) {
            log.warn(
                    "Skipping identity update for deactivated EmberOne user {}",
                    request.getUserId());
            return;
        }

        repository
                .findByEmberoneUserId(request.getUserId())
                .ifPresentOrElse(
                        identityUser -> syncUpdatedUser(request, identityUser),
                        () -> {
                            log.warn(
                                    "Identity user not found for EmberOne user {}. Provisioning.",
                                    request.getUserId());
                            provisionUser(
                                    ProvisionIdentityUserRequest.builder()
                                            .userId(request.getUserId())
                                            .username(request.getUsername())
                                            .firstName(request.getFirstName())
                                            .lastName(request.getLastName())
                                            .privateEmail(request.getPrivateEmail())
                                            .role(EmberOneAppRole.USER.getRoleName())
                                            .tenantId(request.getTenantId())
                                            .build());
                        });
    }

    private void syncUpdatedUser(UpdateIdentityUserRequest request, IdentityUser identityUser) {
        String previousUsername = identityUser.getUsername();

        identityUser.setDisplayName(
                buildDisplayName(request.getFirstName(), request.getLastName()));
        identityUser.setGivenName(request.getFirstName());
        identityUser.setSurname(request.getLastName());
        identityUser.setUsername(request.getUsername());
        identityUser.setPrivateEmail(request.getPrivateEmail());
        identityUser.setAccountEnabled(true);

        if (identityUser.getIdpUserId() == null) {
            IdpUserCreationResult created =
                    identityProviderUserProvisioner.createUser(
                            identityUser, request.getTenantId(), identityUser.getAppRole());
            identityUser.setIdpUserId(created.idpUserId());
            sendWelcomeNotification(
                    request.getTenantId(),
                    request.getUserId(),
                    request.getPrivateEmail(),
                    request.getFirstName(),
                    request.getUsername(),
                    created.temporaryPassword());
        } else {
            identityProviderUserProvisioner.updateUser(
                    identityUser, request.getTenantId(), identityUser.getAppRole());
            if (usernameChanged(previousUsername, request.getUsername())) {
                sendUsernameChangedNotification(request, previousUsername);
            }
        }

        repository.save(identityUser);
        log.info("Updated identity for user {}", request.getUserId());
    }

    private String buildDisplayName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    private void sendWelcomeNotification(
            Long tenantId,
            Long userId,
            String privateEmail,
            String firstName,
            String username,
            String temporaryPassword) {
        if (!StringUtils.hasText(temporaryPassword) || !StringUtils.hasText(privateEmail)) {
            return;
        }
        eventPublisher.publishEvent(
                new UserWelcomeNeededEvent(
                        tenantId, userId, privateEmail, firstName, username, temporaryPassword));
    }

    private void sendUsernameChangedNotification(
            UpdateIdentityUserRequest request, String previousUsername) {
        if (!StringUtils.hasText(request.getUsername())) {
            return;
        }
        String previous = previousUsername != null ? previousUsername.trim() : "";
        String next = request.getUsername().trim();
        if (previous.equals(next)) {
            return;
        }
        eventPublisher.publishEvent(
                new UsernameChangedEvent(
                        request.getTenantId(), request.getUserId(), previous, next));
    }

    @Transactional
    public void updateAppRole(Long tenantId, Long emberoneUserId, String newRole) {
        String normalized = normalizeAppRole(newRole);
        try {
            TenantContextHolder.setTenantId(tenantId);
            IdentityUser identityUser =
                    repository
                            .findByEmberoneUserId(emberoneUserId)
                            .orElseThrow(
                                    () ->
                                            new ResponseStatusException(
                                                    HttpStatus.NOT_FOUND,
                                                    "Identity user not found"));
            if (!tenantId.equals(identityUser.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not in tenant");
            }
            if (isDeactivatedEmberOneUser(emberoneUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Cannot change app role for a deactivated user");
            }
            identityUser.setAppRole(normalized);
            identityProviderUserProvisioner.updateUser(identityUser, tenantId, normalized);
            repository.save(identityUser);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Removes all IdP accounts for a tenant during mandant purge ({@code idm_users} rows are
     * removed by {@code ON DELETE CASCADE} on {@code tenants}).
     */
    public void deprovisionAllIdpUsersForTenantPurge(long tenantId) {
        runWithTenantContext(
                tenantId,
                () -> {
                    for (IdentityUser identityUser :
                            repository.findAllByTenantIdOrderByIdAsc(tenantId)) {
                        if (!StringUtils.hasText(identityUser.getIdpUserId())) {
                            continue;
                        }
                        identityProviderUserProvisioner.deleteUser(identityUser.getIdpUserId());
                    }
                    log.info("Deprovisioned IdP users for tenant purge tenantId={}", tenantId);
                });
    }

    /** Marks identity for asynchronous IdP removal after the surrounding DB transaction commits. */
    @Transactional
    public void scheduleDeprovision(Long tenantId, Long emberoneUserId) {
        runWithTenantContext(
                tenantId,
                () -> {
                    IdentityUser identityUser =
                            repository.findByEmberoneUserId(emberoneUserId).orElse(null);
                    if (identityUser == null) {
                        return;
                    }
                    identityUser.setAccountEnabled(false);
                    identityUser.setDeprovisionPendingAt(Instant.now());
                    repository.save(identityUser);
                    log.info("Scheduled identity deprovision for user {}", emberoneUserId);
                });
    }

    private static void runWithTenantContext(Long tenantId, Runnable action) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            action.run();
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    private boolean isDeactivatedEmberOneUser(Long emberoneUserId) {
        return userRepository
                .findById(emberoneUserId)
                .map(user -> user.getDeactivatedAt() != null)
                .orElse(false);
    }
}
