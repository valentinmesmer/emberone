/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organization.service;

import com.emberone.organization.event.OrganizationCreatedEvent;
import org.junit.jupiter.api.Test;

class OrganizationCreatedListenerTest {

    @Test
    void onOrganizationCreatedAcceptsEventWithoutError() {
        OrganizationCreatedListener listener = new OrganizationCreatedListener();

        listener.onOrganizationCreated(new OrganizationCreatedEvent(7L, 10L, 1L));
    }
}
