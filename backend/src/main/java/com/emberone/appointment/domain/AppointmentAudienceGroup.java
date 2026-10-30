/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_audience_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAudienceGroup {

    @EmbeddedId private AppointmentAudienceGroupId id;

    @MapsId("appointmentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;
}
