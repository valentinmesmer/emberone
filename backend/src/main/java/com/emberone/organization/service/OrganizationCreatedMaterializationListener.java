/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.standard.service.StandardMaterializationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Materializes standard templates for a newly created organization in the same transaction as the
 * create. Must run synchronously (not after-commit) so the org never exists without its groups /
 * permissions / catalog selections.
 */
@Service
@RequiredArgsConstructor
public class OrganizationCreatedMaterializationListener {

    private final OrganizationRepository organizationRepository;
    private final StandardMaterializationService standardMaterializationService;

    @Order(0)
    @EventListener
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        Organization organization =
                organizationRepository
                        .findById(event.organizationId())
                        .orElseThrow(
                                () -> new OrganizationNotFoundException(event.organizationId()));
        standardMaterializationService.materializeForOrg(organization);
    }
}
