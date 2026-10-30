/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.event.event.EventDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventDeletionCertificationGrantCleanupListener {

    private final CertificationEventGrantService certificationEventGrantService;

    /**
     * Runs synchronously in the deleting transaction so grant/record cleanup rolls back together
     * with the event delete if anything fails.
     */
    @EventListener
    public void onEventDeleting(EventDeletingEvent event) {
        certificationEventGrantService.deleteGrantsForEvent(event.eventType(), event.eventId());
    }
}
