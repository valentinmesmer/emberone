/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.service.EmergencyContactService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EmergencyContactOrganizationControllerTest {

    @Mock EmergencyContactService emergencyContactService;

    @InjectMocks EmergencyContactOrganizationController controller;

    @Test
    void getEmergencyContactsForOrganizationDelegatesToService() {
        List<EmergencyContactDto> contacts = List.of(EmergencyContactDto.builder().id(1L).build());
        when(emergencyContactService.getEmergencyContactsForOrganization(10L)).thenReturn(contacts);

        ResponseEntity<List<EmergencyContactDto>> response =
                controller.getEmergencyContactsForOrganization(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(contacts);
        verify(emergencyContactService).getEmergencyContactsForOrganization(10L);
    }
}
