/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.service;

import com.emberone.user.event.UserCreatedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Keeps system-managed group memberships in sync with organization membership events. */
@Service
@RequiredArgsConstructor
public class AllMembersGroupMembershipListener {

    private final AllMembersGroupService allMembersGroupService;

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        allMembersGroupService.ensureUserInAllMembersGroup(event.userId(), event.organizationId());
    }

    @EventListener
    public void onOrganizationMembershipCreated(OrganizationMembershipCreatedEvent event) {
        allMembersGroupService.ensureUserInAllMembersGroup(event.userId(), event.organizationId());
    }
}
