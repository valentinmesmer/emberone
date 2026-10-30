/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type;

import com.emberone.notification.domain.NotificationType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationTypeDefinitionRegistry {

    private final Map<NotificationType, NotificationTypeDefinition> definitions;

    public NotificationTypeDefinitionRegistry(List<NotificationTypeDefinition> definitions) {
        Map<NotificationType, NotificationTypeDefinition> mapped =
                new EnumMap<>(NotificationType.class);
        for (NotificationTypeDefinition definition : definitions) {
            NotificationTypeDefinition previous = mapped.put(definition.type(), definition);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate NotificationTypeDefinition for " + definition.type());
            }
        }
        for (NotificationType type : NotificationType.values()) {
            if (!mapped.containsKey(type)) {
                throw new IllegalStateException("Missing NotificationTypeDefinition for " + type);
            }
        }
        this.definitions = Map.copyOf(mapped);
    }

    public NotificationTypeDefinition require(NotificationType type) {
        NotificationTypeDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalStateException("No NotificationTypeDefinition for " + type);
        }
        return definition;
    }
}
