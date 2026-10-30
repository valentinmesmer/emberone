/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.domain.ManagementActivationEndedBy;
import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.dto.ManagementActivationDto;
import com.emberone.management.dto.ManagementActivationRequest;
import com.emberone.management.dto.ManagementTargetOrganizationDto;
import com.emberone.management.exception.ManagementActivationNotFoundException;
import com.emberone.management.exception.ManagementActivationValidationException;
import com.emberone.management.repository.ManagementActivationRepository;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permission.realtime.PermissionRealtimeNotifier;
import com.emberone.platform.exception.AccessDeniedException;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens and closes management activations. An activation is the grant that the access check reads;
 * creating or ending one changes nothing else.
 *
 * <p>An activation fits when one audience entry of the managing organization covers the target, the
 * requested duration is within that entry's maximum, and the chosen permissions are within that
 * entry's scope (its permission types limited by the target's ceiling: enabled modules, without
 * {@code MANAGEMENT_ACCESS}).
 */
@Service
@RequiredArgsConstructor
public class ManagementActivationService {

    static final int REASON_MIN_LENGTH = 1;
    static final int REASON_MAX_LENGTH = 500;

    private final ManagementActivationRepository activationRepository;
    private final ManagementAudienceRepository audienceRepository;
    private final ManagementCapabilityService capabilityService;
    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final EnabledModuleResolver enabledModuleResolver;
    private final UserPermissionService userPermissionService;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final PermissionRealtimeNotifier permissionRealtimeNotifier;
    private final OrganizationMapper organizationMapper;

    @Transactional
    public ManagementActivationDto activate(
            long tenantId, long userId, ManagementActivationRequest request) {
        String reason = normalizeReason(request.getReason());
        if (request.getPermissionTypes() == null || request.getPermissionTypes().isEmpty()) {
            throw new ManagementActivationValidationException(
                    "At least one permission is required");
        }
        Set<PermissionType> requested = EnumSet.copyOf(request.getPermissionTypes());
        List<Organization> managingOrganizations = managingOrganizationsOf(tenantId, userId);
        Organization managing =
                managingOrganizations.stream()
                        .filter(org -> org.getId().equals(request.getManagingOrganizationId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Not allowed to manage through this organization"));
        Organization target = requireActiveTarget(tenantId, request.getTargetOrganizationId());
        if (target.getId().equals(managing.getId())) {
            throw new ManagementActivationValidationException(
                    "An organization cannot be managed through itself");
        }

        List<Organization> activeOrganizations =
                organizationRepository.findActiveByTenantIdOrderByName(tenantId);
        Set<PermissionType> ceiling = ceilingFor(target);
        boolean fitsOneEntry =
                audienceRepository
                        .findByManagingOrganizationIdOrderByIdAsc(managing.getId())
                        .stream()
                        .filter(entry -> covers(entry, target, activeOrganizations))
                        .anyMatch(
                                entry ->
                                        request.getDurationMinutes()
                                                        <= entry.getMaxDurationMinutes()
                                                && scopeOf(entry, ceiling).containsAll(requested));
        if (!fitsOneEntry) {
            throw new ManagementActivationValidationException(
                    "The target is not covered by the audience, or the duration or permissions exceed it");
        }

        Instant now = Instant.now();
        activationRepository
                .findByUserIdAndTargetOrganizationIdAndEndedAtIsNull(userId, target.getId())
                .ifPresent(previous -> end(previous, ManagementActivationEndedBy.USER, now));
        // Flush the ended row before the insert: the open-row unique index must not see both.
        activationRepository.flush();

        // Whole minutes only: start and end sit on a minute boundary, so the displayed end and the
        // expiry event match and the granted duration is exactly the requested minutes.
        Instant activatedAt = now.truncatedTo(ChronoUnit.MINUTES);
        ManagementActivation saved;
        try {
            saved =
                    activationRepository.saveAndFlush(
                            ManagementActivation.builder()
                                    .tenantId(tenantId)
                                    .userId(userId)
                                    .managingOrganizationId(managing.getId())
                                    .targetOrganizationId(target.getId())
                                    .activatedAt(activatedAt)
                                    .validUntil(
                                            activatedAt.plus(
                                                    Duration.ofMinutes(
                                                            request.getDurationMinutes())))
                                    .permissionTypes(encode(requested))
                                    .reason(reason)
                                    .build());
        } catch (DataIntegrityViolationException e) {
            // Only the open-row index is a concurrent-activation conflict; anything else is a bug.
            if (e.getMostSpecificCause().getMessage() != null
                    && e.getMostSpecificCause()
                            .getMessage()
                            .contains("uk_org_management_activations_open")) {
                throw new ManagementActivationValidationException(
                        "The activation for this organization changed concurrently, try again");
            }
            throw e;
        }
        permissionRealtimeNotifier.notifyPermissionsChanged(tenantId, userId);
        return toDto(saved, managing, target);
    }

    /** Ends an open activation of the caller. Other users' activations are not found. */
    @Transactional
    public void endOwn(long tenantId, long userId, long activationId) {
        ManagementActivation activation =
                activationRepository
                        .findByIdAndUserIdAndEndedAtIsNull(activationId, userId)
                        .orElseThrow(() -> new ManagementActivationNotFoundException(activationId));
        end(activation, ManagementActivationEndedBy.USER, Instant.now());
        permissionRealtimeNotifier.notifyPermissionsChanged(tenantId, userId);
    }

    /**
     * Ends all open activations through one managing organization. Used by the cleanup listener
     * when that organization lost its capability. Returns the affected user ids.
     */
    @Transactional
    public Set<Long> endSystemActivationsThrough(long managingOrganizationId) {
        Instant now = Instant.now();
        Set<Long> users = new HashSet<>();
        for (ManagementActivation activation :
                activationRepository.findByManagingOrganizationIdAndEndedAtIsNull(
                        managingOrganizationId)) {
            end(activation, ManagementActivationEndedBy.SYSTEM, now);
            if (activation.getUserId() != null) {
                users.add(activation.getUserId());
            }
        }
        return users;
    }

    /** Open activations of the caller, earliest expiry first. Display only, not an access check. */
    @Transactional(readOnly = true)
    public List<ManagementActivationDto> listOwnOpen(long tenantId, long userId) {
        return activationRepository
                .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                        tenantId, userId, Instant.now())
                .stream()
                .map(
                        activation ->
                                toDto(
                                        activation,
                                        organizationRepository
                                                .findById(activation.getManagingOrganizationId())
                                                .orElse(null),
                                        organizationRepository
                                                .findById(activation.getTargetOrganizationId())
                                                .orElse(null)))
                .toList();
    }

    /**
     * Master data of a target the caller has an open activation for. Fails closed with "not found"
     * when no open activation covers the target.
     */
    @Transactional(readOnly = true)
    public OrganizationDto openTarget(long tenantId, long userId, long targetOrganizationId) {
        boolean open =
                activationRepository
                        .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                                tenantId, userId, Instant.now())
                        .stream()
                        .anyMatch(
                                activation ->
                                        Objects.equals(
                                                activation.getTargetOrganizationId(),
                                                targetOrganizationId));
        if (!open) {
            throw new OrganizationNotFoundException(targetOrganizationId);
        }
        return organizationMapper.toDto(requireActiveTarget(tenantId, targetOrganizationId));
    }

    /**
     * Organizations the caller may open now, one row per target and managing organization, sorted
     * by target name.
     */
    @Transactional(readOnly = true)
    public List<ManagementTargetOrganizationDto> listTargets(long tenantId, long userId) {
        List<Organization> activeOrganizations =
                organizationRepository.findActiveByTenantIdOrderByName(tenantId);
        List<ManagementTargetOrganizationDto> rows = new ArrayList<>();
        for (Organization managing : managingOrganizationsOf(tenantId, userId)) {
            List<ManagementAudience> entries =
                    audienceRepository.findByManagingOrganizationIdOrderByIdAsc(managing.getId());
            Map<Long, List<ManagementAudience>> coveringByTarget = new LinkedHashMap<>();
            for (Organization target : activeOrganizations) {
                if (target.getId().equals(managing.getId())) {
                    continue;
                }
                List<ManagementAudience> covering =
                        entries.stream()
                                .filter(entry -> covers(entry, target, activeOrganizations))
                                .toList();
                if (!covering.isEmpty()) {
                    coveringByTarget.put(target.getId(), covering);
                }
            }
            for (Organization target : activeOrganizations) {
                List<ManagementAudience> covering = coveringByTarget.get(target.getId());
                if (covering == null) {
                    continue;
                }
                Set<PermissionType> ceiling = ceilingFor(target);
                rows.add(
                        ManagementTargetOrganizationDto.builder()
                                .organizationId(target.getId())
                                .organizationName(target.getName())
                                .managingOrganizationId(managing.getId())
                                .managingOrganizationName(managing.getName())
                                .maxDurationMinutes(
                                        covering.stream()
                                                .mapToInt(ManagementAudience::getMaxDurationMinutes)
                                                .max()
                                                .orElse(0))
                                .permissionTypes(
                                        covering.stream()
                                                .flatMap(entry -> scopeOf(entry, ceiling).stream())
                                                .distinct()
                                                .sorted()
                                                .toList())
                                .build());
            }
        }
        return rows.stream()
                .sorted(
                        Comparator.comparing(ManagementTargetOrganizationDto::getOrganizationName)
                                .thenComparing(
                                        ManagementTargetOrganizationDto
                                                ::getManagingOrganizationName))
                .toList();
    }

    /**
     * Organizations of the tenant where the user is an active member and holds {@code
     * MANAGEMENT_ACCESS} directly, and that are capable of managing. No chains: rights that came
     * through management never count here, the permission map only reports direct grants.
     */
    private List<Organization> managingOrganizationsOf(long tenantId, long userId) {
        UserPermissionDto permissions = userPermissionService.getUserPermissions(userId);
        if (permissions.getPermissions() == null) {
            return List.of();
        }
        Instant now = Instant.now();
        return permissions.getPermissions().entrySet().stream()
                .filter(e -> e.getValue().contains(PermissionType.MANAGEMENT_ACCESS))
                .map(e -> organizationRepository.findById(e.getKey()).orElse(null))
                .filter(Objects::nonNull)
                .filter(org -> tenantId == org.getTenantId() && !org.isDeactivated())
                .filter(
                        org ->
                                userOrganizationMembershipRepository
                                        .findActiveByUser_IdAndOrganization_Id(
                                                userId, org.getId(), now)
                                        .isPresent())
                .filter(capabilityService::isCapable)
                .toList();
    }

    private Organization requireActiveTarget(long tenantId, Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .filter(org -> tenantId == org.getTenantId() && !org.isDeactivated())
                .orElseThrow(
                        () ->
                                new ManagementActivationValidationException(
                                        "Target organization is not available"));
    }

    /** Rights the target can carry: its enabled modules, without {@code MANAGEMENT_ACCESS}. */
    private Set<PermissionType> ceilingFor(Organization target) {
        return ManagementScope.ceilingFor(target, effectiveStandardResolver, enabledModuleResolver);
    }

    private Set<PermissionType> scopeOf(ManagementAudience entry, Set<PermissionType> ceiling) {
        return ManagementScope.scopeOf(entry, ceiling);
    }

    private boolean covers(
            ManagementAudience entry, Organization target, List<Organization> activeOrganizations) {
        return ManagementScope.covers(entry, target, activeOrganizations);
    }

    private void end(
            ManagementActivation activation, ManagementActivationEndedBy endedBy, Instant now) {
        activation.setEndedAt(now);
        activation.setEndedBy(endedBy);
    }

    private ManagementActivationDto toDto(
            ManagementActivation activation, Organization managing, Organization target) {
        return ManagementActivationDto.builder()
                .id(activation.getId())
                .managingOrganizationId(activation.getManagingOrganizationId())
                .managingOrganizationName(managing == null ? null : managing.getName())
                .targetOrganizationId(activation.getTargetOrganizationId())
                .targetOrganizationName(target == null ? null : target.getName())
                .activatedAt(activation.getActivatedAt())
                .validUntil(activation.getValidUntil())
                .permissionTypes(decode(activation.getPermissionTypes()))
                .reason(activation.getReason())
                .build();
    }

    private static String normalizeReason(String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.length() < REASON_MIN_LENGTH || trimmed.length() > REASON_MAX_LENGTH) {
            throw new ManagementActivationValidationException(
                    "Reason must be between "
                            + REASON_MIN_LENGTH
                            + " and "
                            + REASON_MAX_LENGTH
                            + " characters");
        }
        return trimmed;
    }

    static String encode(Set<PermissionType> types) {
        return types.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    static List<PermissionType> decode(String encoded) {
        return Arrays.stream(encoded.split(",")).map(PermissionType::valueOf).sorted().toList();
    }
}
