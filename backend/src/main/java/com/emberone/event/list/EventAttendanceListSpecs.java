/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import com.emberone.event.domain.EventAttendance;
import com.emberone.event.domain.EventType;
import org.springframework.data.jpa.domain.Specification;

public final class EventAttendanceListSpecs {

    private EventAttendanceListSpecs() {}

    public static Specification<EventAttendance> forEvent(EventType eventType, Long eventId) {
        return (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("eventType"), eventType),
                        cb.equal(root.get("eventId"), eventId));
    }
}
