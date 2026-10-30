/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.client.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class AuthentikUserRequest {
    private String username;
    private String name;

    /** Notification / contact email (Authentik user email field). */
    private String email;

    private boolean isActive;
    private Map<String, List<String>> attributes;

    /**
     * Authentik group PKs (UUID strings); used for the mutually exclusive EmberOne application
     * roles.
     */
    private List<String> groups;
}
