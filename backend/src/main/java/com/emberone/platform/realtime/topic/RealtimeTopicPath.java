/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.topic;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code /topic/tenant.{tenantId}.{domain}} and {@code
 * /topic/tenant.{tenantId}.{domain}.{entityId}} destinations.
 */
public final class RealtimeTopicPath {

    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^/topic/tenant\\.(\\d+)\\.([^.]+)(?:\\.(\\d+))?$");

    private final long tenantId;
    private final String domain;
    private final Optional<Long> entityId;

    private RealtimeTopicPath(long tenantId, String domain, Optional<Long> entityId) {
        this.tenantId = tenantId;
        this.domain = domain;
        this.entityId = entityId;
    }

    public static Optional<RealtimeTopicPath> parse(String destination) {
        if (destination == null || destination.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = TOPIC_PATTERN.matcher(destination.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        long tenantId = Long.parseLong(matcher.group(1));
        String domain = matcher.group(2);
        String entityIdGroup = matcher.group(3);
        Optional<Long> entityId =
                entityIdGroup == null
                        ? Optional.empty()
                        : Optional.of(Long.parseLong(entityIdGroup));
        return Optional.of(new RealtimeTopicPath(tenantId, domain, entityId));
    }

    public long tenantId() {
        return tenantId;
    }

    public String domain() {
        return domain;
    }

    public Optional<Long> entityId() {
        return entityId;
    }

    public boolean isTenantTopic() {
        return entityId.isEmpty();
    }
}
