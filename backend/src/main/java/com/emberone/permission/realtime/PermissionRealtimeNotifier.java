/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.realtime;

import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionRealtimeNotifier {

    public static final String EVENT_PERMISSIONS_CHANGED = "permissions.changed";

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;

    public void notifyPermissionsChanged(Long tenantId, Long userId) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null || tenantId == null || userId == null) {
            return;
        }
        publisher.publishToUser(
                tenantId,
                userId,
                RealtimePublisher.PERMISSIONS_QUEUE,
                RealtimeEvent.thin(EVENT_PERMISSIONS_CHANGED, Map.of()));
    }
}
