/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.service;

import com.emberone.userorganizationmembership.event.OrganizationMembershipEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Ends organization-group memberships in the same transaction as the org-membership end, so {@code
 * endProcessedAt} rolls back together with the cascade if either fails.
 */
@Service
@RequiredArgsConstructor
public class OrganizationMembershipEndedGroupListener {

    private final UserOrganizationGroupMembershipService groupMembershipService;

    @EventListener
    public void onOrganizationMembershipEnded(OrganizationMembershipEndedEvent event) {
        groupMembershipService.endAllActiveMembershipsForUserInOrganization(
                event.userId(), event.organizationId(), event.endDate());
    }
}
