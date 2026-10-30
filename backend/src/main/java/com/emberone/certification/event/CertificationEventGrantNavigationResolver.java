/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

import com.emberone.event.domain.EventType;
import com.emberone.event.util.EventCategoryPath;
import com.emberone.platform.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves grant id → event page path (+ event type + org). Lives here because {@code
 * AppNavigationResolver} cannot do the DB lookup without a platform→certification dependency.
 *
 * <p>App-Link {@code CERTIFICATION_EVENT_GRANT} navigates to the event (service/operation), not the
 * grant-admin detail. Grant detail stays reachable only from the event UI.
 */
@Component
@RequiredArgsConstructor
public class CertificationEventGrantNavigationResolver {

    private final CertificationEventGrantRepository grantRepository;
    private final TokenService tokenService;

    private static String eventDetailPagePath(CertificationEventGrant grant) {
        return "/organizations/"
                + grant.getOrganizationId()
                + "/department/"
                + EventCategoryPath.fromEventType(grant.getEventType())
                + "/"
                + grant.getEventId();
    }

    public Resolved resolve(Long grantId) {
        if (grantId == null) {
            return null;
        }
        Long tenantId = tokenService.getTenantId();
        if (tenantId == null) {
            return null;
        }
        return grantRepository
                .findById(grantId)
                .map(
                        grant ->
                                new Resolved(
                                        eventDetailPagePath(grant),
                                        grant.getEventType(),
                                        grant.getOrganizationId()))
                .orElse(null);
    }

    public record Resolved(String navigationPath, EventType eventType, Long organizationId) {}
}
