/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.topic;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicAuthorizationService {

    private final List<TopicAuthorizer> authorizers;

    private static boolean isUserQueueDestination(String destination) {
        return destination.startsWith("/user/queue/");
    }

    public boolean authorizeSubscribe(EmberOnePrincipal principal, String destination) {
        if (destination == null || destination.isBlank()) {
            return false;
        }
        if (isUserQueueDestination(destination)) {
            return true;
        }
        return RealtimeTopicPath.parse(destination)
                .map(topic -> authorizeTopic(principal, topic))
                .orElse(false);
    }

    private boolean authorizeTopic(EmberOnePrincipal principal, RealtimeTopicPath topic) {
        if (principal.getTenantId() != topic.tenantId()) {
            return false;
        }
        if (topic.isTenantTopic()) {
            return true;
        }
        return authorizers.stream()
                .filter(authorizer -> authorizer.supports(topic.domain()))
                .findFirst()
                .map(
                        authorizer ->
                                authorizer.authorize(principal, topic.domain(), topic.entityId()))
                .orElse(false);
    }
}
