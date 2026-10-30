/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.realtime;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagementActivationRealtimeNotifier {

    public static final String EVENT_ACTIVATION_EXPIRED = "management.activation.expired";

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;

    /** Tells the activating user that the management access ended by its validity. */
    public void notifyExpired(ManagementActivation activation) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null || activation.getUserId() == null) {
            return;
        }
        publisher.publishToUser(
                activation.getTenantId(),
                activation.getUserId(),
                RealtimePublisher.PERMISSIONS_QUEUE,
                RealtimeEvent.thin(
                        EVENT_ACTIVATION_EXPIRED,
                        Map.of(
                                "activationId", activation.getId(),
                                "managingOrganizationId", activation.getManagingOrganizationId(),
                                "targetOrganizationId", activation.getTargetOrganizationId())));
    }
}
