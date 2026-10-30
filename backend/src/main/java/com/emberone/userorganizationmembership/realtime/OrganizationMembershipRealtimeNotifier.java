/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.realtime;

import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Thin list-topic fan-out for org membership changes. No entity topic - clients invalidate
 * organization user lists via REST re-fetch (same pattern as appointments/events list domains).
 */
@Component
@RequiredArgsConstructor
public class OrganizationMembershipRealtimeNotifier {

    public static final String EVENT_CHANGED = "organization-membership.changed";
    static final String LIST_DOMAIN = "organization-memberships";

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;

    public void notifyChanged(Long tenantId, Long organizationId, Long userId) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null || tenantId == null || organizationId == null || userId == null) {
            return;
        }
        Map<String, Long> entityIds = new LinkedHashMap<>();
        entityIds.put("organizationId", organizationId);
        entityIds.put("userId", userId);
        publisher.publishToTopic(
                tenantId, LIST_DOMAIN, RealtimeEvent.thin(EVENT_CHANGED, entityIds));
    }
}
