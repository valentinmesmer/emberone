/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.service;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.dto.OrganizationGroupMemberDto;
import com.emberone.organizationgroup.dto.OrganizationGroupMemberPageDto;
import com.emberone.organizationgroup.exception.OrganizationGroupNotFoundException;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UpdateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipActivatedEvent;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipDeactivatedEvent;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipAlreadyExistsException;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipNotFoundException;
import com.emberone.userorganizationgroupmembership.list.OrganizationGroupMemberListDefinition;
import com.emberone.userorganizationgroupmembership.list.OrganizationGroupMemberListSpecs;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.MembershipTimestampSupport;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrganizationGroupMembershipService {

    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final UserOrganizationGroupMembershipMapper mapper;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final AllMembersGroupService allMembersGroupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserOrganizationGroupMembershipDto createMembership(
            CreateUserOrganizationGroupMembershipRequest request) {
        return createMembershipInternal(request, true);
    }

    /**
     * Creates a membership in a system-managed group (e.g. leadership). Used by tenant admin flows
     * only.
     */
    @Transactional
    public UserOrganizationGroupMembershipDto createSystemManagedMembership(
            CreateUserOrganizationGroupMembershipRequest request) {
        return createMembershipInternal(request, false);
    }

    private UserOrganizationGroupMembershipDto createMembershipInternal(
            CreateUserOrganizationGroupMembershipRequest request,
            boolean validateManualModificationAllowed) {
        User user =
                userRepository
                        .findById(request.getUserId())
                        .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        OrganizationGroup organizationGroup =
                organizationGroupRepository
                        .findById(request.getOrganizationGroupId())
                        .orElseThrow(
                                () ->
                                        new OrganizationGroupNotFoundException(
                                                request.getOrganizationGroupId()));

        Long organizationId = organizationGroup.getOrganization().getId();
        if (!userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                request.getUserId(), organizationId, Instant.now())) {
            throw new IllegalArgumentException(
                    "User does not belong to the specified organization");
        }

        if (validateManualModificationAllowed) {
            allMembersGroupService.validateMembershipModificationAllowed(organizationGroup);
        }

        validateWritableDateRange(request.getStartDate(), request.getEndDate());

        List<UserOrganizationGroupMembership> existingMemberships =
                groupMembershipRepository.findByUser_IdAndOrganizationGroup_Id(
                        request.getUserId(), request.getOrganizationGroupId());

        boolean hasOverlap =
                existingMemberships.stream()
                        .anyMatch(
                                existing ->
                                        MembershipTimestampSupport.rangesOverlap(
                                                existing.getStartDate(),
                                                existing.getEndDate(),
                                                request.getStartDate(),
                                                request.getEndDate()));

        if (hasOverlap) {
            throw new UserOrganizationGroupMembershipAlreadyExistsException(
                    request.getUserId(), request.getOrganizationGroupId());
        }

        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .tenantId(user.getTenantId())
                        .user(user)
                        .organizationGroup(organizationGroup)
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .membershipType(request.getMembershipType())
                        .build();

        UserOrganizationGroupMembership saved = groupMembershipRepository.save(membership);

        log.info(
                "Created organization group membership {} for user {} in group {} with type {}",
                saved.getId(),
                request.getUserId(),
                request.getOrganizationGroupId(),
                request.getMembershipType());

        Instant now = Instant.now();
        if (saved.isActiveAt(now)) {
            publishMembershipActivated(request.getUserId(), request.getOrganizationGroupId());
        }
        eventPublisher.publishEvent(
                new UserPermissionsChangedEvent(user.getTenantId(), request.getUserId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public UserOrganizationGroupMembershipDto updateMembership(
            Long membershipId, UpdateUserOrganizationGroupMembershipRequest request) {
        return updateMembershipInternal(membershipId, request, true);
    }

    /**
     * Updates a membership in a system-managed group (e.g. leadership). Used by tenant admin flows
     * only.
     */
    @Transactional
    public UserOrganizationGroupMembershipDto updateSystemManagedMembership(
            Long membershipId, UpdateUserOrganizationGroupMembershipRequest request) {
        return updateMembershipInternal(membershipId, request, false);
    }

    private UserOrganizationGroupMembershipDto updateMembershipInternal(
            Long membershipId,
            UpdateUserOrganizationGroupMembershipRequest request,
            boolean validateManualModificationAllowed) {
        UserOrganizationGroupMembership membership =
                groupMembershipRepository
                        .findById(membershipId)
                        .orElseThrow(
                                () ->
                                        new UserOrganizationGroupMembershipNotFoundException(
                                                membershipId));

        if (validateManualModificationAllowed) {
            allMembersGroupService.validateMembershipModificationAllowed(
                    membership.getOrganizationGroup());
        }

        Long userId = membership.getUser().getId();
        Long organizationGroupId = membership.getOrganizationGroup().getId();
        Instant now = Instant.now();
        boolean wasActive = membership.isActiveAt(now);

        if (request.getMembershipType() != membership.getMembershipType()) {
            return changeMembershipType(membership, request, now);
        }

        validateWritableDateRange(request.getStartDate(), request.getEndDate());
        requireNoOverlap(membership, request.getStartDate(), request.getEndDate());

        mapper.updateFromRequest(request, membership);

        boolean isActive = membership.isActiveAt(now);
        if (wasActive && !isActive) {
            membership.setEndProcessedAt(now);
        }
        UserOrganizationGroupMembership saved = groupMembershipRepository.save(membership);

        syncMembershipLifecycleEvents(
                saved.getTenantId(), userId, organizationGroupId, wasActive, isActive);

        log.info(
                "Updated organization group membership {} for user {} in group {}",
                membershipId,
                userId,
                organizationGroupId);

        eventPublisher.publishEvent(new UserPermissionsChangedEvent(saved.getTenantId(), userId));
        return mapper.toDto(saved);
    }

    /**
     * A role change (e.g. leader to member) closes the current membership at {@code now} and opens
     * a new one with the new type, so the earlier role stays in the history. The request's start
     * date is not applied; the new membership starts at {@code now}. The user stays in the group,
     * so no activation/deactivation events are published.
     */
    private UserOrganizationGroupMembershipDto changeMembershipType(
            UserOrganizationGroupMembership current,
            UpdateUserOrganizationGroupMembershipRequest request,
            Instant now) {
        if (!current.isActiveAt(now)) {
            throw new IllegalArgumentException(
                    "Membership type of an ended membership cannot be changed");
        }
        validateWritableDateRange(now, request.getEndDate());

        Long userId = current.getUser().getId();
        current.setEndDate(now);
        current.setEndProcessedAt(now);
        groupMembershipRepository.save(current);

        UserOrganizationGroupMembership next =
                groupMembershipRepository.save(
                        UserOrganizationGroupMembership.builder()
                                .tenantId(current.getTenantId())
                                .user(current.getUser())
                                .organizationGroup(current.getOrganizationGroup())
                                .startDate(now)
                                .endDate(request.getEndDate())
                                .membershipType(request.getMembershipType())
                                .build());

        log.info(
                "Changed membership type of user {} in group {} from {} to {} (membership {} -> {})",
                userId,
                current.getOrganizationGroup().getId(),
                current.getMembershipType(),
                request.getMembershipType(),
                current.getId(),
                next.getId());

        eventPublisher.publishEvent(new UserPermissionsChangedEvent(current.getTenantId(), userId));
        return mapper.toDto(next);
    }

    private void requireNoOverlap(
            UserOrganizationGroupMembership membership, Instant startDate, Instant endDate) {
        Long userId = membership.getUser().getId();
        Long organizationGroupId = membership.getOrganizationGroup().getId();
        boolean hasOverlap =
                groupMembershipRepository
                        .findByUser_IdAndOrganizationGroup_Id(userId, organizationGroupId)
                        .stream()
                        .filter(m -> !m.getId().equals(membership.getId()))
                        .anyMatch(
                                existing ->
                                        MembershipTimestampSupport.rangesOverlap(
                                                existing.getStartDate(),
                                                existing.getEndDate(),
                                                startDate,
                                                endDate));
        if (hasOverlap) {
            throw new UserOrganizationGroupMembershipAlreadyExistsException(
                    userId, organizationGroupId);
        }
    }

    /**
     * Ends the user's active membership in a group. System groups (all-members and leadership)
     * cannot be left this way; custom groups can (used by approved leave workflows).
     */
    @Transactional
    public void endActiveMembership(Long userId, Long organizationGroupId, Instant endDate) {
        Instant now = Instant.now();
        Instant effectiveEndDate = endDate != null ? endDate : now;
        UserOrganizationGroupMembership membership =
                groupMembershipRepository
                        .findActiveByUser_IdAndOrganizationGroup_Id(
                                userId, organizationGroupId, now)
                        .orElseThrow(
                                () ->
                                        new UserOrganizationGroupMembershipNotFoundException(
                                                organizationGroupId));

        OrganizationGroup group = membership.getOrganizationGroup();
        allMembersGroupService.validateMembershipModificationAllowed(group);

        MembershipTimestampSupport.requireEndAfterStart(
                membership.getStartDate(), effectiveEndDate);

        boolean wasActive = membership.isActiveAt(now);
        membership.setEndDate(effectiveEndDate);
        if (wasActive && !membership.isActiveAt(now)) {
            membership.setEndProcessedAt(now);
            publishMembershipDeactivated(membership.getTenantId(), userId, organizationGroupId);
        }
        groupMembershipRepository.save(membership);
        eventPublisher.publishEvent(
                new UserPermissionsChangedEvent(membership.getTenantId(), userId));
        log.info(
                "Ended organization group membership {} for user {} in group {}",
                membership.getId(),
                userId,
                organizationGroupId);
    }

    @Transactional
    public void endAllActiveMembershipsForUserInOrganization(
            Long userId, Long organizationId, Instant endDate) {
        Instant effectiveEndDate = endDate != null ? endDate : Instant.now();
        Instant now = Instant.now();
        List<UserOrganizationGroupMembership> memberships =
                groupMembershipRepository.findActiveByUser_IdAndOrganization_Id(
                        userId, organizationId, effectiveEndDate);

        for (UserOrganizationGroupMembership membership : memberships) {
            Long groupId = membership.getOrganizationGroup().getId();
            boolean wasActive = membership.isActiveAt(now);
            membership.setEndDate(effectiveEndDate);
            if (wasActive && !membership.isActiveAt(now)) {
                membership.setEndProcessedAt(now);
                publishMembershipDeactivated(membership.getTenantId(), userId, groupId);
            }
            groupMembershipRepository.save(membership);
            log.info(
                    "Ended organization group membership {} for user {} as part of organization membership end",
                    membership.getId(),
                    userId);
        }

        if (!memberships.isEmpty()) {
            Long tenantId = memberships.getFirst().getTenantId();
            eventPublisher.publishEvent(new UserPermissionsChangedEvent(tenantId, userId));
        }
    }

    /**
     * Applies pending deactivation side effects for group memberships whose {@code endDate} has
     * elapsed. Safe after downtime: unprocessed rows are picked up regardless of how long ago they
     * ended.
     */
    @Transactional
    public void processDueEndedMemberships() {
        Instant now = Instant.now();
        List<UserOrganizationGroupMembership> due =
                groupMembershipRepository.findDueEndedUnprocessed(now);
        for (UserOrganizationGroupMembership membership : due) {
            Long userId = membership.getUser().getId();
            Long organizationGroupId = membership.getOrganizationGroup().getId();
            membership.setEndProcessedAt(now);
            groupMembershipRepository.save(membership);
            publishMembershipDeactivated(membership.getTenantId(), userId, organizationGroupId);
            eventPublisher.publishEvent(
                    new UserPermissionsChangedEvent(membership.getTenantId(), userId));
            log.info(
                    "Processed due ended organization group membership {} for user {} in group {}",
                    membership.getId(),
                    userId,
                    organizationGroupId);
        }
        if (!due.isEmpty()) {
            log.info("Processed {} due ended organization group membership(s)", due.size());
        }
    }

    @Transactional(readOnly = true)
    public OrganizationGroupMemberPageDto pageMembersByOrganizationGroupId(
            Long organizationGroupId, ListQuery query) {
        Instant now = Instant.now();
        Pageable pageable = OrganizationGroupMemberListDefinition.INSTANCE.pageable(query);
        Specification<UserOrganizationGroupMembership> spec =
                Specification.allOf(
                        OrganizationGroupMemberListSpecs.forOrganizationGroup(organizationGroupId),
                        OrganizationGroupMemberListSpecs.activeAt(now),
                        OrganizationGroupMemberListDefinition.INSTANCE.searchSpecification(query));
        Page<UserOrganizationGroupMembership> page =
                groupMembershipRepository.findAll(spec, pageable);
        List<OrganizationGroupMemberDto> content =
                page.getContent().stream().map(this::toOrganizationGroupMemberDto).toList();
        return OrganizationGroupMemberPageDto.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    public List<OrganizationGroupMemberDto> getMembersByOrganizationGroupId(
            Long organizationGroupId) {
        List<UserOrganizationGroupMembership> memberships =
                groupMembershipRepository.findActiveByOrganizationGroup_IdWithUser(
                        organizationGroupId, Instant.now());
        return memberships.stream()
                .map(this::toOrganizationGroupMemberDto)
                .sorted(
                        Comparator.comparing(
                                        OrganizationGroupMemberDto::getLastName,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        OrganizationGroupMemberDto::getFirstName,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(OrganizationGroupMemberDto::getId))
                .collect(Collectors.toList());
    }

    private OrganizationGroupMemberDto toOrganizationGroupMemberDto(
            UserOrganizationGroupMembership membership) {
        User user = membership.getUser();
        return OrganizationGroupMemberDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private void syncMembershipLifecycleEvents(
            Long tenantId,
            Long userId,
            Long organizationGroupId,
            boolean wasActive,
            boolean isActive) {
        if (wasActive && !isActive) {
            publishMembershipDeactivated(tenantId, userId, organizationGroupId);
        } else if (!wasActive && isActive) {
            publishMembershipActivated(userId, organizationGroupId);
        }
    }

    private static void validateWritableDateRange(Instant startDate, Instant endDate) {
        MembershipTimestampSupport.requireStartNotInTheFuture(startDate);
        MembershipTimestampSupport.requireEndAfterStart(startDate, endDate);
    }

    private void publishMembershipActivated(Long userId, Long organizationGroupId) {
        eventPublisher.publishEvent(
                new OrganizationGroupMembershipActivatedEvent(userId, organizationGroupId));
    }

    private void publishMembershipDeactivated(
            Long tenantId, Long userId, Long organizationGroupId) {
        eventPublisher.publishEvent(
                new OrganizationGroupMembershipDeactivatedEvent(
                        tenantId, userId, organizationGroupId));
    }
}
