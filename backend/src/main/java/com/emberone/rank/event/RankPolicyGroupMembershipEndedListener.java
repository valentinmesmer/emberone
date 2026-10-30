/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.event;

import com.emberone.federalstate.FederalStateService;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankPolicyGroupMembershipEndedListener {

    private final FederalStateService federalStateService;

    @AfterCommitEventListener
    public void onMembershipEnded(OrganizationGroupMembershipDeactivatedEvent event) {
        if (event.tenantId() == null) {
            return;
        }
        federalStateService
                .settings(federalStateService.resolveTenantFederalState(event.tenantId()))
                .rankPolicy()
                .onGroupMembershipEnded(
                        event.tenantId(), event.userId(), event.organizationGroupId());
    }
}
