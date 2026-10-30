/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.topic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RealtimeTopicPathTest {

    @Test
    void parsesTenantTopic() {
        var topic = RealtimeTopicPath.parse("/topic/tenant.5.appointments").orElseThrow();

        assertThat(topic.tenantId()).isEqualTo(5L);
        assertThat(topic.domain()).isEqualTo("appointments");
        assertThat(topic.entityId()).isEmpty();
        assertThat(topic.isTenantTopic()).isTrue();
    }

    @Test
    void parsesEntityTopic() {
        var topic = RealtimeTopicPath.parse("/topic/tenant.5.workflow.123").orElseThrow();

        assertThat(topic.tenantId()).isEqualTo(5L);
        assertThat(topic.domain()).isEqualTo("workflow");
        assertThat(topic.entityId()).contains(123L);
    }

    @Test
    void rejectsInvalidDestination() {
        assertThat(RealtimeTopicPath.parse("/topic/workflows/1")).isEmpty();
    }
}
