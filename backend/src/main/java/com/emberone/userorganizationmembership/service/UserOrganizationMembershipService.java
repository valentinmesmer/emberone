/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserAccountContinuity;
import com.emberone.userorganizationmembership.domain.MembershipTimestampSupport;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.CreateUserOrganizationMembershipRequest;
import com.emberone.userorganizationmembership.dto.UpdateUserOrganizationMembershipRequest;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.event.OrganizationMembershipCreatedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipEndedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipUpdatedEvent;
import com.emberone.userorganizationmembership.exception.OrganizationMembershipAlreadyEndedException;
import com.emberone.userorganizationmembership.exception.OrganizationMembershipEndBlockedByLeadershipException;
import com.emberone.userorganizationmembership.exception.UserOrganizationMembershipAlreadyExistsException;
import com.emberone.userorganizationmembership.exception.UserOrganizationMembershipNotFoundException;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrganizationMembershipService {

    private final UserOrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationMembershipMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AllMembersGroupService allMembersGroupService;
    private final UserAccountContinuity userAccountContinuity;

    @Transactional
    public UserOrganizationMembershipDto createMembership(
            CreateUserOrganizationMembershipRequest request) {
        User user =
                userRepository
                        .findById(request.getUserId())
                        .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        Organization organization =
                organizationRepository
                        .findById(request.getOrganizationId())
                        .orElseThrow(
                                () ->
                                        new OrganizationNotFoundException(
                                                request.getOrganizationId()));

        if (!user.getTenantId().equals(organization.getTenantId())) {
            throw new IllegalArgumentException(
                    "User and organization must belong to the same tenant");
        }

        MembershipTimestampSupport.requireStartNotInTheFuture(request.getStartDate());
        validateNoOverlap(
                request.getUserId(),
                request.getOrganizationId(),
                request.getStartDate(),
                null,
                null);

        UserOrganizationMembership membership =
                UserOrganizationMembership.builder()
                        .tenantId(user.getTenantId())
                        .user(user)
                        .organization(organization)
                        .main(request.isMain())
                        .startDate(request.getStartDate())
                        .build();

        UserOrganizationMembership saved = membershipRepository.save(membership);
        log.info(
                "Created organization membership {} for user {} in organization {}",
                saved.getId(),
                request.getUserId(),
                request.getOrganizationId());

        eventPublisher.publishEvent(
                new OrganizationMembershipCreatedEvent(
                        user.getTenantId(), request.getUserId(), request.getOrganizationId()));
        eventPublisher.publishEvent(
                new UserPermissionsChangedEvent(user.getTenantId(), request.getUserId()));
        return mapper.toDto(saved);
    }

    /**
     * Switches the user's main organization to an existing active membership. Clears the previous
     * main flag when one exists.
     */
    @Transactional
    public void changeMainOrganization(Long userId, Long newMainOrganizationId) {
        Instant now = Instant.now();
        Optional<UserOrganizationMembership> currentMain =
                membershipRepository.findActiveByUser_IdAndMainTrue(userId, now);

        UserOrganizationMembership newMainMembership =
                membershipRepository
                        .findActiveByUser_IdAndOrganization_Id(userId, newMainOrganizationId, now)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User is not an active member of organization "
                                                        + newMainOrganizationId));

        Long previousMainOrganizationId = null;
        if (currentMain.isPresent()) {
            UserOrganizationMembership current = currentMain.get();
            previousMainOrganizationId = current.getOrganization().getId();
            if (previousMainOrganizationId.equals(newMainOrganizationId)) {
                throw new IllegalArgumentException(
                        "New main organization must differ from current");
            }
            current.setMain(false);
            membershipRepository.save(current);
        }

        newMainMembership.setMain(true);
        UserOrganizationMembership saved = membershipRepository.save(newMainMembership);

        log.info(
                "Changed main organization for user {} from {} to {}",
                userId,
                previousMainOrganizationId,
                newMainOrganizationId);
        if (previousMainOrganizationId != null) {
            eventPublisher.publishEvent(
                    new OrganizationMembershipUpdatedEvent(
                            saved.getTenantId(), userId, previousMainOrganizationId));
        }
        eventPublisher.publishEvent(
                new OrganizationMembershipUpdatedEvent(
                        saved.getTenantId(), userId, newMainOrganizationId));
        eventPublisher.publishEvent(new UserPermissionsChangedEvent(saved.getTenantId(), userId));
    }

    @Transactional
    public UserOrganizationMembershipDto updateMembership(
            Long membershipId, UpdateUserOrganizationMembershipRequest request) {
        UserOrganizationMembership membership =
                membershipRepository
                        .findById(membershipId)
                        .orElseThrow(
                                () ->
                                        new UserOrganizationMembershipNotFoundException(
                                                membershipId));

        Long userId = membership.getUser().getId();
        Long organizationId = membership.getOrganization().getId();
        Instant now = Instant.now();
        boolean wasActive = membership.isActiveAt(now);

        rejectChangeWhileInLeadership(userId, organizationId);
        MembershipTimestampSupport.requireStartNotInTheFuture(request.getStartDate());

        validateNoOverlap(
                userId,
                organizationId,
                request.getStartDate(),
                membership.getEndDate(),
                membershipId);

        membership.setStartDate(request.getStartDate());

        UserOrganizationMembership saved = membershipRepository.save(membership);
        log.info(
                "Updated organization membership {} for user {} in organization {}",
                membershipId,
                userId,
                organizationId);
        if (wasActive && !saved.isActiveAt(now)) {
            applyEndedEffects(saved, now);
        } else {
            eventPublisher.publishEvent(
                    new OrganizationMembershipUpdatedEvent(
                            saved.getTenantId(), userId, organizationId));
            eventPublisher.publishEvent(
                    new UserPermissionsChangedEvent(saved.getTenantId(), userId));
        }
        return mapper.toDto(saved);
    }

    /**
     * Sets {@code endDate}. Rejected while the user is in the organization's leadership group -
     * tenant admin must end that leadership membership first. Org-membership updates are rejected
     * for the same reason. Side effects run immediately when that date is already in the past;
     * otherwise {@link #processDueEndedMemberships()} applies them once the date elapses.
     */
    @Transactional
    public UserOrganizationMembershipDto endMembership(Long membershipId, Instant endDate) {
        UserOrganizationMembership membership =
                membershipRepository
                        .findById(membershipId)
                        .orElseThrow(
                                () ->
                                        new UserOrganizationMembershipNotFoundException(
                                                membershipId));

        Long userId = membership.getUser().getId();
        Long organizationId = membership.getOrganization().getId();
        Instant now = Instant.now();
        Instant effectiveEndDate = endDate != null ? endDate : now;

        if (!membership.isActiveAt(now)) {
            throw new OrganizationMembershipAlreadyEndedException(membershipId);
        }

        rejectChangeWhileInLeadership(userId, organizationId);
        MembershipTimestampSupport.requireEndAfterStart(
                membership.getStartDate(), effectiveEndDate);
        userAccountContinuity.requireSustainableAfterEndingOrganizationMembership(
                userId, organizationId, effectiveEndDate);

        membership.setEndDate(effectiveEndDate);
        UserOrganizationMembership saved = membershipRepository.save(membership);
        log.info(
                "Ended organization membership {} for user {} in organization {}",
                membershipId,
                userId,
                organizationId);

        if (!saved.isActiveAt(now)) {
            applyEndedEffects(saved, now);
        } else {
            eventPublisher.publishEvent(
                    new OrganizationMembershipUpdatedEvent(
                            saved.getTenantId(), userId, organizationId));
            eventPublisher.publishEvent(
                    new UserPermissionsChangedEvent(saved.getTenantId(), userId));
        }
        return mapper.toDto(saved);
    }

    /**
     * Ends an active organization membership as part of user deactivation. Skips the leadership
     * guard because group memberships (including leadership) are ended first.
     */
    @Transactional
    public void endActiveMembershipForUserDeactivation(
            UserOrganizationMembership membership, Instant endDate) {
        Instant now = Instant.now();
        Instant effectiveEndDate = endDate != null ? endDate : now;
        if (!membership.isActiveAt(now)) {
            return;
        }
        MembershipTimestampSupport.requireEndAfterStart(
                membership.getStartDate(), effectiveEndDate);
        membership.setEndDate(effectiveEndDate);
        UserOrganizationMembership saved = membershipRepository.save(membership);
        log.info(
                "Ended organization membership {} for user {} in organization {} (user deactivation)",
                saved.getId(),
                saved.getUser().getId(),
                saved.getOrganization().getId());
        if (!saved.isActiveAt(now)) {
            applyEndedEffects(saved, now);
        }
    }

    /**
     * Applies pending end side effects for every membership whose {@code endDate} has elapsed. Safe
     * after downtime: unprocessed rows are picked up regardless of how long ago they ended.
     */
    @Transactional
    public void processDueEndedMemberships() {
        Instant now = Instant.now();
        List<UserOrganizationMembership> due = membershipRepository.findDueEndedUnprocessed(now);
        for (UserOrganizationMembership membership : due) {
            applyEndedEffects(membership, now);
            log.info(
                    "Processed due ended organization membership {} for user {} in organization {}",
                    membership.getId(),
                    membership.getUser().getId(),
                    membership.getOrganization().getId());
        }
        if (!due.isEmpty()) {
            log.info("Processed {} due ended organization membership(s)", due.size());
        }
    }

    private void applyEndedEffects(UserOrganizationMembership membership, Instant now) {
        if (membership.getEndProcessedAt() != null) {
            return;
        }
        Long userId = membership.getUser().getId();
        Long organizationId = membership.getOrganization().getId();
        membership.setEndProcessedAt(now);
        membershipRepository.save(membership);
        eventPublisher.publishEvent(
                new OrganizationMembershipEndedEvent(
                        membership.getTenantId(), userId, organizationId, membership.getEndDate()));
        eventPublisher.publishEvent(
                new UserPermissionsChangedEvent(membership.getTenantId(), userId));
    }

    private void rejectChangeWhileInLeadership(Long userId, Long organizationId) {
        if (allMembersGroupService.hasActiveLeadershipMembership(userId, organizationId)) {
            throw new OrganizationMembershipEndBlockedByLeadershipException();
        }
    }

    private void validateNoOverlap(
            Long userId,
            Long organizationId,
            Instant newStart,
            Instant newEnd,
            Long excludeMembershipId) {
        List<UserOrganizationMembership> existingMemberships =
                membershipRepository.findByUser_IdAndOrganization_Id(userId, organizationId);

        boolean hasOverlap =
                existingMemberships.stream()
                        .filter(m -> !m.getId().equals(excludeMembershipId))
                        .anyMatch(
                                existing ->
                                        MembershipTimestampSupport.rangesOverlap(
                                                existing.getStartDate(),
                                                existing.getEndDate(),
                                                newStart,
                                                newEnd));

        if (hasOverlap) {
            throw new UserOrganizationMembershipAlreadyExistsException(userId, organizationId);
        }
    }
}
