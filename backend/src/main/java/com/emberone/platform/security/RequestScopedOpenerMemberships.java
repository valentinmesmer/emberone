/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.security;

import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Membership facts of the current user, loaded at most once per HTTP request. Listing the
 * beginnable workflow types evaluates {@link WorkflowOpenAccess} once per handler; without this the
 * same two user-level queries would run for every handler.
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class RequestScopedOpenerMemberships {

    private final UserOrganizationMembershipRepository membershipRepository;

    private Boolean hasActiveMembership;
    private Boolean hasActiveMainMembership;

    public boolean hasActiveMembership(Long userId) {
        if (hasActiveMembership == null) {
            hasActiveMembership = membershipRepository.existsActiveByUser_Id(userId, Instant.now());
        }
        return hasActiveMembership;
    }

    public boolean hasActiveMainMembership(Long userId) {
        if (hasActiveMainMembership == null) {
            hasActiveMainMembership =
                    membershipRepository
                            .findActiveByUser_IdAndMainTrue(userId, Instant.now())
                            .isPresent();
        }
        return hasActiveMainMembership;
    }
}
