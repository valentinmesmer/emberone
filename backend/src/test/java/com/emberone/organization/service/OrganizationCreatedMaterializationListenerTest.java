/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.standard.service.StandardMaterializationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationCreatedMaterializationListenerTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock StandardMaterializationService standardMaterializationService;
    @InjectMocks OrganizationCreatedMaterializationListener listener;

    @Test
    void onOrganizationCreatedMaterializesStandardForOrg() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));

        listener.onOrganizationCreated(new OrganizationCreatedEvent(7L, 10L, 1L));

        verify(standardMaterializationService).materializeForOrg(organization);
    }
}
