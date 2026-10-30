/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import java.security.Principal;
import java.util.Objects;
import lombok.Getter;
import org.springframework.security.oauth2.jwt.Jwt;

@Getter
public final class EmberOnePrincipal implements Principal {

    private final Long tenantId;
    private final Long userId;
    private final Jwt jwt;
    private final String name;

    public EmberOnePrincipal(Long tenantId, Long userId, Jwt jwt) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.jwt = Objects.requireNonNull(jwt, "jwt");
        this.name = formatName(tenantId, userId);
    }

    public static String formatName(Long tenantId, Long userId) {
        return tenantId + ":" + userId;
    }

    @Override
    public String getName() {
        return name;
    }
}
