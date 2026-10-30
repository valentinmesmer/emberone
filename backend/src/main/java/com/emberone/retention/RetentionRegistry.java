/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.exception.RetentionConfigurationException;
import com.emberone.retention.profile.RetentionProfile;
import com.emberone.retention.spi.RetentionHandler;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetentionRegistry {

    private final Map<RetentionCategory, RetentionHandler> handlersByCategory;

    /** Profiles validate themselves on construction ({@link RetentionProfile#of}). */
    public RetentionRegistry(List<RetentionHandler> handlers) {
        Map<RetentionCategory, RetentionHandler> mapped = new EnumMap<>(RetentionCategory.class);
        for (RetentionHandler handler : handlers) {
            RetentionHandler previous = mapped.put(handler.category(), handler);
            if (previous != null) {
                throw new RetentionConfigurationException(
                        "Duplicate retention handler for " + handler.category());
            }
        }
        for (RetentionCategory category : RetentionCategory.values()) {
            if (!mapped.containsKey(category)) {
                throw new RetentionConfigurationException(
                        "Missing retention handler for " + category);
            }
        }
        this.handlersByCategory = Map.copyOf(mapped);

        log.debug(
                "Registered {} retention handler(s): {}",
                handlersByCategory.size(),
                handlersByCategory.keySet());
    }

    public RetentionHandler require(RetentionCategory category) {
        RetentionHandler handler = handlersByCategory.get(category);
        if (handler == null) {
            throw new RetentionConfigurationException("No retention handler for " + category);
        }
        return handler;
    }

    public List<RetentionHandler> all() {
        return Arrays.stream(RetentionCategory.values()).map(handlersByCategory::get).toList();
    }
}
