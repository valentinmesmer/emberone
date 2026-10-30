/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.platform.security.TokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EmergencyContactSelfControllerTest {

    @Mock EmergencyContactService emergencyContactService;
    @Mock TokenService tokenService;

    EmergencyContactSelfController controller;

    @BeforeEach
    void setUp() {
        controller = new EmergencyContactSelfController(emergencyContactService, tokenService);
    }

    @Test
    void createEmergencyContactReturnsCreatedForAuthenticatedUser() {
        when(tokenService.requireUserId()).thenReturn(8L);
        CreateEmergencyContactRequest request = new CreateEmergencyContactRequest();
        EmergencyContactDto created = EmergencyContactDto.builder().id(1L).build();
        when(emergencyContactService.createEmergencyContact(8L, request)).thenReturn(created);

        ResponseEntity<EmergencyContactDto> response = controller.createEmergencyContact(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void getEmergencyContactsDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(8L);
        List<EmergencyContactDto> contacts = List.of(EmergencyContactDto.builder().id(2L).build());
        when(emergencyContactService.getEmergencyContactsForUser(8L)).thenReturn(contacts);

        ResponseEntity<List<EmergencyContactDto>> response = controller.getEmergencyContacts();

        assertThat(response.getBody()).isEqualTo(contacts);
    }
}
