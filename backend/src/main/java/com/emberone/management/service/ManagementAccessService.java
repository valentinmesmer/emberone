/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.repository.ManagementActivationRepository;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.RequestScopedUserPermissions;
import com.emberone.platform.security.TokenService;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Live rights of the current user in organizations they manage through an open activation. The only
 * place that turns an activation into rights; {@code OrganizationAccessService} calls it after the
 * direct check. Nothing is cached across requests: every request re-reads the managing
 * organization, its capability, the audience and the target, so a change takes effect at once.
 *
 * <p>Rights = chosen permissions ∩ (permission types of the covering entries ∩ modules of the
 * target) without {@code MANAGEMENT_ACCESS}. Any exception or missing precondition yields no rights
 * (fail-closed).
 */
@Slf4j
@Service
@RequestScope
@RequiredArgsConstructor
public class ManagementAccessService {

    private final TokenService tokenService;
    private final RequestScopedUserPermissions requestScopedUserPermissions;
    private final ManagementActivationRepository activationRepository;
    private final ManagementAudienceRepository audienceRepository;
    private final ManagementCapabilityService capabilityService;
    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final EnabledModuleResolver enabledModuleResolver;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;

    private List<ManagementActivation> openActivations;
    private List<Organization> activeOrganizations;
    private final Map<Long, Set<PermissionType>> rightsByTarget = new HashMap<>();

    /**
     * Rights the current user holds in the target through management. Empty when none apply. Runs
     * in one transaction: the permission types of a group are lazy and open-in-view is off, so
     * reading them outside a transaction would throw and fail closed for every target.
     */
    @Transactional(readOnly = true)
    public Set<PermissionType> rightsIn(long targetOrganizationId) {
        Long tenantId = tokenService.getTenantId();
        Long userId = tokenService.getUserId();
        if (tenantId == null || userId == null) {
            return Set.of();
        }
        return rightsByTarget.computeIfAbsent(
                targetOrganizationId, target -> evaluate(tenantId, userId, target));
    }

    private Set<PermissionType> evaluate(long tenantId, long userId, long targetOrganizationId) {
        try {
            return openActivations(tenantId, userId).stream()
                    .filter(
                            activation ->
                                    activation.getTargetOrganizationId() == targetOrganizationId)
                    .findFirst()
                    .map(activation -> rightsOf(activation, tenantId, userId))
                    .orElseGet(() -> EnumSet.noneOf(PermissionType.class));
        } catch (RuntimeException e) {
            log.warn("Management access check failed closed, target={}", targetOrganizationId, e);
            return EnumSet.noneOf(PermissionType.class);
        }
    }

    private Set<PermissionType> rightsOf(
            ManagementActivation activation, long tenantId, long userId) {
        Set<PermissionType> none = EnumSet.noneOf(PermissionType.class);
        Organization managing =
                activeOrganization(activation.getManagingOrganizationId(), tenantId);
        Organization target = activeOrganization(activation.getTargetOrganizationId(), tenantId);
        if (managing == null || target == null || !holdsManagementDirectly(managing)) {
            return none;
        }
        if (!isActiveMember(userId, managing.getId()) || !capabilityService.isCapable(managing)) {
            return none;
        }

        List<Organization> active = activeOrganizations(tenantId);
        Set<PermissionType> ceiling =
                ManagementScope.ceilingFor(
                        target, effectiveStandardResolver, enabledModuleResolver);
        Set<PermissionType> granted = EnumSet.noneOf(PermissionType.class);
        audienceRepository.findByManagingOrganizationIdOrderByIdAsc(managing.getId()).stream()
                .filter(entry -> ManagementScope.covers(entry, target, active))
                .forEach(entry -> granted.addAll(ManagementScope.scopeOf(entry, ceiling)));
        if (granted.isEmpty()) {
            return none;
        }

        Set<PermissionType> rights = EnumSet.noneOf(PermissionType.class);
        for (PermissionType type :
                ManagementActivationService.decode(activation.getPermissionTypes())) {
            if (type != PermissionType.MANAGEMENT_ACCESS && granted.contains(type)) {
                rights.add(type);
            }
        }
        log.info(
                "Management access: actor={} managing={} target={} activation={}",
                userId,
                managing.getId(),
                target.getId(),
                activation.getId());
        return rights;
    }

    /** Managing rights come only from a direct grant, never from management itself. */
    private boolean holdsManagementDirectly(Organization managing) {
        var permissions = requestScopedUserPermissions.getUnfiltered().getPermissions();
        if (permissions == null) {
            return false;
        }
        var orgPermissions = permissions.get(managing.getId());
        return orgPermissions != null && orgPermissions.contains(PermissionType.MANAGEMENT_ACCESS);
    }

    private boolean isActiveMember(long userId, long organizationId) {
        return userOrganizationMembershipRepository
                .findActiveByUser_IdAndOrganization_Id(userId, organizationId, Instant.now())
                .isPresent();
    }

    private List<ManagementActivation> openActivations(long tenantId, long userId) {
        if (openActivations == null) {
            openActivations =
                    activationRepository
                            .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                                    tenantId, userId, Instant.now());
        }
        return openActivations;
    }

    private List<Organization> activeOrganizations(long tenantId) {
        if (activeOrganizations == null) {
            activeOrganizations = organizationRepository.findActiveByTenantIdOrderByName(tenantId);
        }
        return activeOrganizations;
    }

    private Organization activeOrganization(Long organizationId, long tenantId) {
        return organizationRepository
                .findById(organizationId)
                .filter(org -> Objects.equals(tenantId, org.getTenantId()) && !org.isDeactivated())
                .orElse(null);
    }
}
