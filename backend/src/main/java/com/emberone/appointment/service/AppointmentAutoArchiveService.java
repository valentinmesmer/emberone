/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.repository.AppointmentRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marks appointments as archived once they ended at least four hours ago (see product rules). */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentAutoArchiveService {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public void archiveEndedAppointments() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofHours(4));
        int updated = appointmentRepository.archiveWhereEndedLongEnoughAgo(cutoff, now);
        if (updated > 0) {
            log.info("Auto-archived {} appointment(s) (end <= {})", updated, cutoff);
        }
    }
}
