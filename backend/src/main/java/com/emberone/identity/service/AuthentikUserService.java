/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import com.emberone.identity.client.AuthentikApiClient;
import com.emberone.identity.client.dto.*;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.exception.IdentityException;
import com.emberone.identity.provider.IdentityProviderKind;
import com.emberone.identity.provider.IdentityProviderUserProvisioner;
import com.emberone.identity.provider.IdpUserCreationResult;
import com.emberone.platform.security.EmberOneAppRole;
import feign.FeignException;
import java.security.SecureRandom;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "emberone.identity",
        name = "provider",
        havingValue = "authentik",
        matchIfMissing = true)
public class AuthentikUserService implements IdentityProviderUserProvisioner {

    private static final String ROLE_GROUP_PREFIX = "role:";

    private final AuthentikApiClient authentikApiClient;

    @Value("${authentik.api.token:}")
    private String apiToken;

    /**
     * Replaces any EmberOne {@code role:*} membership with exactly one matching group, and keeps
     * all other Authentik groups unchanged.
     */
    static List<String> mergeEmberOneRoleMembership(
            List<String> currentGroupPks,
            String normalizedEmberOneRole,
            Map<String, String> roleGroupNameToPk) {

        Set<String> emberoneRolePks = new HashSet<>(roleGroupNameToPk.values());
        String targetName = emberoneRoleGroupName(normalizedEmberOneRole);
        String targetPk = requirePk(roleGroupNameToPk, targetName);

        List<String> next = new ArrayList<>();
        for (String pk : currentGroupPks) {
            if (pk != null && !pk.isBlank() && !emberoneRolePks.contains(pk)) {
                next.add(pk);
            }
        }
        if (!next.contains(targetPk)) {
            next.add(targetPk);
        }
        return next;
    }

    private static String requirePk(Map<String, String> roleGroupNameToPk, String groupName) {
        String pk = roleGroupNameToPk.get(groupName);
        if (pk == null || pk.isBlank()) {
            throw new IdentityException("Missing Authentik group mapping for " + groupName);
        }
        return pk;
    }

    private static String emberoneRoleGroupName(String normalizedEmberOneRole) {
        return ROLE_GROUP_PREFIX + normalizedEmberOneRole;
    }

    private static Optional<String> notificationEmailForAuthentik(IdentityUser identityUser) {
        if (identityUser == null || !StringUtils.hasText(identityUser.getPrivateEmail())) {
            return Optional.empty();
        }
        return Optional.of(identityUser.getPrivateEmail().trim());
    }

    @Override
    public IdentityProviderKind kind() {
        return IdentityProviderKind.AUTHENTIK;
    }

    @Override
    public IdpUserCreationResult createUser(IdentityUser identityUser, Long tenantId, String role) {
        requireAuthentikApiToken("cannot create Authentik user");

        try {
            String tempPassword = generateTemporaryPassword();
            Map<String, String> roleGroupPksByName = loadEmberOneRoleGroupPksByName();
            String normalizedRole = normalizeRole(role);
            List<String> initialGroups =
                    List.of(requirePk(roleGroupPksByName, emberoneRoleGroupName(normalizedRole)));

            AuthentikUserResponse created =
                    authentikApiClient.createUser(
                            toCreateRequest(identityUser, tenantId, role, initialGroups));

            if (created == null || created.getPk() == null) {
                throw new IdentityException("Authentik returned empty user response");
            }

            String userPk = created.getPk().toString();
            setPassword(userPk, tempPassword);
            log.debug(
                    "Created Authentik user {} for EmberOne user {} with role group {}",
                    userPk,
                    identityUser.getEmberoneUserId(),
                    emberoneRoleGroupName(normalizedRole));
            return new IdpUserCreationResult(userPk, tempPassword);
        } catch (IdentityException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Failed to create Authentik user for EmberOne user {}",
                    identityUser.getEmberoneUserId(),
                    e);
            throw new IdentityException("Failed to create Authentik user", e);
        }
    }

    @Override
    public void updateUser(IdentityUser identityUser, Long tenantId, String role) {
        if (identityUser.getIdpUserId() == null) {
            return;
        }
        requireAuthentikApiToken("cannot update Authentik user");

        try {
            Map<String, String> roleGroupPksByName = loadEmberOneRoleGroupPksByName();
            String normalizedRole = normalizeRole(role);
            AuthentikUserDetailResponse remote =
                    authentikApiClient.getUser(identityUser.getIdpUserId());
            if (remote == null) {
                throw new IdentityException(
                        "Authentik user not found: " + identityUser.getIdpUserId());
            }
            List<String> mergedGroups =
                    mergeEmberOneRoleMembership(
                            remote.groupPks(), normalizedRole, roleGroupPksByName);

            authentikApiClient.updateUser(
                    identityUser.getIdpUserId(),
                    toUpdateRequest(identityUser, tenantId, role, mergedGroups));
            log.debug(
                    "Updated Authentik user {} for EmberOne user {} (role group {})",
                    identityUser.getIdpUserId(),
                    identityUser.getEmberoneUserId(),
                    emberoneRoleGroupName(normalizedRole));
        } catch (IdentityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update Authentik user {}", identityUser.getIdpUserId(), e);
            throw new IdentityException("Failed to update Authentik user", e);
        }
    }

    @Override
    public void deleteUser(String idpUserId) {
        if (idpUserId == null || idpUserId.isBlank()) {
            return;
        }
        requireAuthentikApiToken("cannot delete Authentik user");

        try {
            authentikApiClient.deleteUser(idpUserId);
            log.debug("Deleted Authentik user {}", idpUserId);
        } catch (FeignException.NotFound e) {
            log.info("Authentik user {} already deleted", idpUserId);
        } catch (IdentityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete Authentik user {}", idpUserId, e);
            throw new IdentityException("Failed to delete Authentik user", e);
        }
    }

    private void setPassword(String idpUserId, String password) {
        if (idpUserId == null || idpUserId.isBlank()) {
            throw new IdentityException("IdP user id is required");
        }
        requireAuthentikApiToken("cannot set Authentik password");

        try {
            authentikApiClient.setPassword(
                    idpUserId, AuthentikSetPasswordRequest.builder().password(password).build());
            log.debug("Set password for Authentik user {}", idpUserId);
        } catch (IdentityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to set password for Authentik user {}", idpUserId, e);
            throw new IdentityException("Failed to set password", e);
        }
    }

    private Map<String, String> loadEmberOneRoleGroupPksByName() {
        Map<String, String> map = new LinkedHashMap<>();
        for (EmberOneAppRole role : EmberOneAppRole.values()) {
            String groupName = emberoneRoleGroupName(role.getRoleName());
            map.put(groupName, requireGroupPkForExactName(groupName));
        }
        return Map.copyOf(map);
    }

    private String requireGroupPkForExactName(String groupName) {
        AuthentikGroupListResponse response = authentikApiClient.listGroupsByExactName(groupName);
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            throw new IdentityException("Authentik group \"%s\" not found.".formatted(groupName));
        }
        return response.getResults().stream()
                .filter(row -> groupName.equals(row.getName()))
                .findFirst()
                .map(AuthentikGroupRow::pkAsString)
                .orElseThrow(
                        () ->
                                new IdentityException(
                                        "Authentik returned no group named \"%s\" (filter mismatch). Check Authentik API filters."
                                                .formatted(groupName)));
    }

    private AuthentikUserRequest toCreateRequest(
            IdentityUser identityUser, Long tenantId, String role, List<String> groups) {

        String normalizedRole = normalizeRole(role);
        Map<String, List<String>> attributes = new java.util.HashMap<>();
        attributes.put("emberone_user_id", List.of(identityUser.getEmberoneUserId().toString()));
        attributes.put("emberone_role", List.of(normalizedRole));
        if (tenantId != null) {
            attributes.put("emberone_tenant_id", List.of(tenantId.toString()));
        }
        AuthentikUserRequest.AuthentikUserRequestBuilder builder =
                AuthentikUserRequest.builder()
                        .username(identityUser.getUsername())
                        .name(identityUser.getDisplayName())
                        .isActive(identityUser.getAccountEnabled())
                        .attributes(attributes)
                        .groups(groups);
        notificationEmailForAuthentik(identityUser).ifPresent(builder::email);
        return builder.build();
    }

    private AuthentikUserRequest toUpdateRequest(
            IdentityUser identityUser, Long tenantId, String role, List<String> groups) {

        Map<String, List<String>> attributes = new java.util.HashMap<>();
        attributes.put("emberone_user_id", List.of(identityUser.getEmberoneUserId().toString()));
        if (tenantId != null) {
            attributes.put("emberone_tenant_id", List.of(tenantId.toString()));
        }
        String normalizedRole = normalizeRole(role);
        if (role != null && !role.isBlank()) {
            attributes.put("emberone_role", List.of(normalizedRole));
        }
        AuthentikUserRequest.AuthentikUserRequestBuilder builder =
                AuthentikUserRequest.builder()
                        .username(identityUser.getUsername())
                        .name(identityUser.getDisplayName())
                        .isActive(identityUser.getAccountEnabled())
                        .attributes(attributes)
                        .groups(groups);
        notificationEmailForAuthentik(identityUser).ifPresent(builder::email);
        return builder.build();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        String normalized = role.trim().toUpperCase();
        return switch (normalized) {
            case "GLOBAL_ADMIN", "TENANT_ADMIN", "USER" -> normalized;
            default -> "USER";
        };
    }

    private String generateTemporaryPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String allChars = uppercase + lowercase + numbers;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(16);

        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));

        for (int i = 3; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    private void requireAuthentikApiToken(String context) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IdentityException(
                    "Authentik API token is not configured (%s). Set authentik.api.token or environment variable AUTHENTIK_API_TOKEN."
                            .formatted(context));
        }
    }
}
