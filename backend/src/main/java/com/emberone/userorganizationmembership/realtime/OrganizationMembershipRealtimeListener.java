/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.realtime;

import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.userorganizationmembership.event.OrganizationMembershipCreatedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipEndedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationMembershipRealtimeListener {

    private final OrganizationMembershipRealtimeNotifier notifier;

    @AfterCommitCallback
    public void onCreated(OrganizationMembershipCreatedEvent event) {
        notifier.notifyChanged(event.tenantId(), event.organizationId(), event.userId());
    }

    @AfterCommitCallback
    public void onUpdated(OrganizationMembershipUpdatedEvent event) {
        notifier.notifyChanged(event.tenantId(), event.organizationId(), event.userId());
    }

    @AfterCommitCallback
    public void onEnded(OrganizationMembershipEndedEvent event) {
        notifier.notifyChanged(event.tenantId(), event.organizationId(), event.userId());
    }
}
