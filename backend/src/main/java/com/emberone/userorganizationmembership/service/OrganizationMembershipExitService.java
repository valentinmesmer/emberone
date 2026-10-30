/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.service;

import com.emberone.user.service.UserDeactivationService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies approved organization exit workflows (single org vs full account). */
@Service
@RequiredArgsConstructor
public class OrganizationMembershipExitService {

    private final UserOrganizationMembershipService membershipService;
    private final UserDeactivationService userDeactivationService;

    @Transactional
    public void applyApprovedExit(UserOrganizationMembership membership) {
        if (Boolean.TRUE.equals(membership.getMain())) {
            userDeactivationService.deactivateUser(membership.getUser().getId());
            return;
        }
        membershipService.endMembership(membership.getId(), Instant.now());
    }
}
