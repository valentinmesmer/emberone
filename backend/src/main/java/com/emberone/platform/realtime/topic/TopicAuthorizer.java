/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.topic;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import java.util.Optional;

/**
 * Authorizes STOMP SUBSCRIBE on entity topics ({@code /topic/tenant.{t}.{domain}.{entityId}}).
 * Implementations must set {@link com.emberone.platform.security.TenantContextHolder} before DB
 * access.
 */
public interface TopicAuthorizer {

    boolean supports(String domain);

    boolean authorize(EmberOnePrincipal principal, String domain, Optional<Long> entityId);
}
