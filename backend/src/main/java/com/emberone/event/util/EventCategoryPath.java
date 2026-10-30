/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.util;

import com.emberone.event.domain.EventType;
import lombok.experimental.UtilityClass;

/**
 * Maps URL path segments (see {@link com.emberone.event.controller.EventController}) to {@link
 * EventType}.
 */
@UtilityClass
public class EventCategoryPath {

    public static EventType toEventType(String segment) {
        if (segment == null) {
            throw new IllegalArgumentException("Event category is required");
        }
        return switch (segment) {
            case "service" -> EventType.SERVICE;
            case "operation" -> EventType.OPERATION;
            default -> throw new IllegalArgumentException("Unknown event category: " + segment);
        };
    }

    public static String fromEventType(EventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        return switch (eventType) {
            case SERVICE -> "service";
            case OPERATION -> "operation";
        };
    }
}
