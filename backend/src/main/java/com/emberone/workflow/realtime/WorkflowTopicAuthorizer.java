/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.realtime;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.realtime.topic.AbstractTenantScopedTopicAuthorizer;
import com.emberone.platform.security.WorkflowInstanceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowTopicAuthorizer extends AbstractTenantScopedTopicAuthorizer {

    static final String DOMAIN = "workflow";

    private final WorkflowInstanceAccess workflowInstanceAccess;

    @Override
    public boolean supports(String domain) {
        return DOMAIN.equals(domain);
    }

    @Override
    protected boolean authorizeEntity(EmberOnePrincipal principal, Long entityId) {
        return workflowInstanceAccess.canView(entityId, principal.getUserId());
    }
}
