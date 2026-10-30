/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.cache;

import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.userorganizationmembership.event.OrganizationMembershipCreatedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipEndedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationMembershipCacheEvictionListener {

    private final ActiveOrganizationMembershipCacheEvictor activeOrganizationMembershipCacheEvictor;

    @AfterCommitCallback
    public void onOrganizationMembershipCreated(OrganizationMembershipCreatedEvent event) {
        evict(event.userId(), event.organizationId());
    }

    @AfterCommitCallback
    public void onOrganizationMembershipUpdated(OrganizationMembershipUpdatedEvent event) {
        evict(event.userId(), event.organizationId());
    }

    @AfterCommitCallback
    public void onOrganizationMembershipEnded(OrganizationMembershipEndedEvent event) {
        evict(event.userId(), event.organizationId());
    }

    private void evict(Long userId, Long organizationId) {
        if (userId == null || organizationId == null) {
            return;
        }
        activeOrganizationMembershipCacheEvictor.evict(userId, organizationId);
    }
}
