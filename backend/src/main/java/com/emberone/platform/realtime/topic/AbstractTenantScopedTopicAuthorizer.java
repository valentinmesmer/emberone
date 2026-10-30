/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.topic;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TenantContextSupport;
import java.util.Optional;

/** Sets {@link TenantContextHolder} around entity-scoped topic authorization. */
public abstract class AbstractTenantScopedTopicAuthorizer implements TopicAuthorizer {

    @Override
    public boolean authorize(EmberOnePrincipal principal, String domain, Optional<Long> entityId) {
        if (!supports(domain) || entityId.isEmpty()) {
            return false;
        }
        TenantContextSupport.bindFromPrincipal(principal);
        try {
            return authorizeEntity(principal, entityId.get());
        } finally {
            TenantContextSupport.clearMdc();
            TenantContextHolder.clear();
        }
    }

    protected abstract boolean authorizeEntity(EmberOnePrincipal principal, Long entityId);
}
