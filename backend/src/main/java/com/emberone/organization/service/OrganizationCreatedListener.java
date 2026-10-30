/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.platform.event.AfterCommitCallback;
import org.springframework.stereotype.Service;

/**
 * After-commit post-provisioning hook for newly created organizations (notifications, cache warmup,
 * …). Standard materialization runs synchronously via {@link
 * OrganizationCreatedMaterializationListener}.
 */
@Service
public class OrganizationCreatedListener {

    @AfterCommitCallback
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        // Post-provisioning hooks (notifications, cache warmup, …) may be added here.
    }
}
