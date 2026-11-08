/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import java.util.List;

/**
 * Validated suggestion from the help chat. Every value comes from the user's own server-side data,
 * never from model text; the frontend builds the link from {@code kind} + key via a fixed map.
 *
 * <ul>
 *   <li>{@code WORKFLOW}: start a request. {@code workflowTypeId}/{@code workflowTypeKey} from the
 *       beginnable types, {@code configKey}/{@code catalogId}/{@code label} from the catalog.
 *   <li>{@code TOPIC}: open a page. {@code topicKey}/{@code area} from {@link HelpTopic}; {@code
 *       create} opens the page's create dialog; {@code organizations} are the organizations the
 *       user may use for this (more than one → the frontend lets the user choose); {@code tenantId}
 *       for tenant admin pages.
 * </ul>
 */
public record HelpChatActionDto(
        String kind,
        Long workflowTypeId,
        String workflowTypeKey,
        String configKey,
        Long catalogId,
        String label,
        String topicKey,
        HelpTopicArea area,
        boolean create,
        List<HelpChatOrganizationDto> organizations,
        Long tenantId) {

    public static final String KIND_WORKFLOW = "WORKFLOW";
    public static final String KIND_TOPIC = "TOPIC";

    public static HelpChatActionDto workflow(
            long workflowTypeId,
            String workflowTypeKey,
            String configKey,
            Long catalogId,
            String label) {
        return new HelpChatActionDto(
                KIND_WORKFLOW,
                workflowTypeId,
                workflowTypeKey,
                configKey,
                catalogId,
                label,
                null,
                null,
                false,
                List.of(),
                null);
    }

    public static HelpChatActionDto topic(
            AvailableHelpTopic available,
            boolean create,
            List<HelpChatOrganizationDto> organizations) {
        return new HelpChatActionDto(
                KIND_TOPIC,
                null,
                null,
                null,
                null,
                null,
                available.topic().name(),
                available.topic().getArea(),
                create,
                List.copyOf(organizations),
                available.tenantId());
    }
}
