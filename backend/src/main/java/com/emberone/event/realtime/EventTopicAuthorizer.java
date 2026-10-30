/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.realtime;

import com.emberone.event.domain.EventType;
import com.emberone.event.repository.OperationEventRepository;
import com.emberone.event.repository.ServiceEventRepository;
import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.realtime.topic.AbstractTenantScopedTopicAuthorizer;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TenantContextSupport;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventTopicAuthorizer extends AbstractTenantScopedTopicAuthorizer {

    static final String SERVICE_DOMAIN = "service-event";
    static final String OPERATION_DOMAIN = "operation-event";

    private final ServiceEventRepository serviceEventRepository;
    private final OperationEventRepository operationEventRepository;
    private final SecurityService securityService;

    @Override
    public boolean supports(String domain) {
        return SERVICE_DOMAIN.equals(domain) || OPERATION_DOMAIN.equals(domain);
    }

    @Override
    public boolean authorize(EmberOnePrincipal principal, String domain, Optional<Long> entityId) {
        if (!supports(domain) || entityId.isEmpty()) {
            return false;
        }
        EventType eventType =
                SERVICE_DOMAIN.equals(domain) ? EventType.SERVICE : EventType.OPERATION;
        Long organizationId =
                resolveOrganizationId(eventType, entityId.get(), principal.getTenantId());
        if (organizationId == null) {
            return false;
        }
        TenantContextSupport.bindFromPrincipal(principal);
        try {
            String category = eventType == EventType.SERVICE ? "service" : "operation";
            return securityService.hasEventCategoryReadPermission(organizationId, category)
                    || securityService.hasEventAttendanceReadPermission(organizationId);
        } finally {
            TenantContextSupport.clearMdc();
            TenantContextHolder.clear();
        }
    }

    @Override
    protected boolean authorizeEntity(EmberOnePrincipal principal, Long entityId) {
        return false;
    }

    private Long resolveOrganizationId(EventType eventType, Long eventId, Long tenantId) {
        return switch (eventType) {
            case SERVICE ->
                    serviceEventRepository
                            .findById(eventId)
                            .filter(event -> tenantId.equals(event.getTenantId()))
                            .map(event -> event.getOrganization().getId())
                            .orElse(null);
            case OPERATION ->
                    operationEventRepository
                            .findById(eventId)
                            .filter(event -> tenantId.equals(event.getTenantId()))
                            .map(event -> event.getOrganization().getId())
                            .orElse(null);
        };
    }
}
