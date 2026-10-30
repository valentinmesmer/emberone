/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingMainOrganizationSkipEvaluator {

    private final UserOrganizationMembershipRepository membershipRepository;

    public boolean hasOtherActiveMainOrganization(long userId, long targetOrganizationId) {
        return membershipRepository
                .findActiveByUser_IdAndMainTrue(userId, Instant.now())
                .filter(membership -> membership.getOrganization().getId() != targetOrganizationId)
                .isPresent();
    }
}
