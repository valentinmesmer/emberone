/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.identity.service.IdentityUserService;
import com.emberone.user.domain.User;
import com.emberone.user.event.UserDeactivatedEvent;
import com.emberone.user.exception.UserAlreadyDeactivatedException;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeactivationService {

    private final UserRepository userRepository;
    private final UserOrganizationMembershipRepository organizationMembershipRepository;
    private final UserOrganizationMembershipService organizationMembershipService;
    private final UserOrganizationGroupMembershipService groupMembershipService;
    private final IdentityUserService identityUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deactivateUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getDeactivatedAt() != null) {
            throw new UserAlreadyDeactivatedException(userId);
        }

        Instant now = Instant.now();
        List<UserOrganizationMembership> activeMemberships =
                organizationMembershipRepository.findActiveByUser_Id(userId, now);

        for (UserOrganizationMembership membership : activeMemberships) {
            Long organizationId = membership.getOrganization().getId();
            groupMembershipService.endAllActiveMembershipsForUserInOrganization(
                    userId, organizationId, now);
            organizationMembershipService.endActiveMembershipForUserDeactivation(membership, now);
        }

        user.setDeactivatedAt(now);
        userRepository.save(user);

        identityUserService.scheduleDeprovision(user.getTenantId(), userId);

        eventPublisher.publishEvent(new UserDeactivatedEvent(user.getTenantId(), userId));
        log.info("Deactivated user {}", userId);
    }

    @Transactional(readOnly = true)
    public boolean hasDeactivationPending(Long userId) {
        return userRepository
                .findById(userId)
                .map(user -> user.getDeactivatedAt() != null)
                .orElse(false);
    }
}
