/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.event;

import com.emberone.event.domain.EventType;

/**
 * Published while a service or operation event is still present, immediately before the row is
 * removed. Sync listeners clean dependents in the same transaction; after-commit listeners (e.g.
 * realtime) may also subscribe - the STOMP payload remains {@code event.deleted}.
 */
public record EventDeletingEvent(Long tenantId, EventType eventType, Long eventId) {}
