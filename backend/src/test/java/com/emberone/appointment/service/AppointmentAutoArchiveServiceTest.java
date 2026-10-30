/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.emberone.appointment.repository.AppointmentRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentAutoArchiveServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @InjectMocks AppointmentAutoArchiveService service;

    @Captor ArgumentCaptor<Instant> cutoffCaptor;
    @Captor ArgumentCaptor<Instant> archivedAtCaptor;

    @Test
    void archivesAppointmentsUsingFourHourCutoff() {
        Instant beforeCutoff = Instant.now().minus(Duration.ofHours(4)).minusSeconds(1);
        Instant beforeArchivedAt = Instant.now().minusSeconds(1);

        service.archiveEndedAppointments();

        Instant afterCutoff = Instant.now().minus(Duration.ofHours(4)).plusSeconds(1);
        Instant afterArchivedAt = Instant.now().plusSeconds(1);
        verify(appointmentRepository)
                .archiveWhereEndedLongEnoughAgo(cutoffCaptor.capture(), archivedAtCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBetween(beforeCutoff, afterCutoff);
        assertThat(archivedAtCaptor.getValue()).isBetween(beforeArchivedAt, afterArchivedAt);
    }
}
