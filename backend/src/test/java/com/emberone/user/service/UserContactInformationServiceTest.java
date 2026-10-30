/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.repository.UserContactInformationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserContactInformationServiceTest {

    @Mock UserContactInformationRepository contactInformationRepository;

    @Test
    void returnsPrivateEmailWhenPresent() {
        when(contactInformationRepository.findByUser_Id(5L))
                .thenReturn(
                        Optional.of(
                                UserContactInformation.builder()
                                        .privateEmail("ada@example.com")
                                        .build()));

        UserContactInformationService service =
                new UserContactInformationService(contactInformationRepository);

        assertThat(service.findPrivateEmail(5L)).contains("ada@example.com");
    }

    @Test
    void returnsEmptyWhenEmailIsBlankOrContactMissing() {
        when(contactInformationRepository.findByUser_Id(5L))
                .thenReturn(
                        Optional.of(UserContactInformation.builder().privateEmail(" ").build()));
        when(contactInformationRepository.findByUser_Id(6L)).thenReturn(Optional.empty());

        UserContactInformationService service =
                new UserContactInformationService(contactInformationRepository);

        assertThat(service.findPrivateEmail(5L)).isEmpty();
        assertThat(service.findPrivateEmail(6L)).isEmpty();
    }
}
