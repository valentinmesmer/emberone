/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import java.util.List;

/**
 * A {@link HelpTopic} the current user may use, resolved in their context.
 *
 * @param readOrganizations organization topics: organizations where the user may open the page
 * @param createOrganizations organization topics: subset where the user may also create
 * @param tenantId admin topics scoped to a tenant: the tenant from the token
 */
public record AvailableHelpTopic(
        HelpTopic topic,
        List<HelpChatOrganizationDto> readOrganizations,
        List<HelpChatOrganizationDto> createOrganizations,
        Long tenantId) {

    public AvailableHelpTopic {
        readOrganizations = List.copyOf(readOrganizations);
        createOrganizations = List.copyOf(createOrganizations);
    }

    /** Whether a create link may be offered at all. */
    public boolean canCreate() {
        return switch (topic.getArea()) {
            case SELF -> topic.isSelfCreate();
            case ORGANIZATION -> !createOrganizations.isEmpty();
            case ADMIN -> false;
        };
    }
}
