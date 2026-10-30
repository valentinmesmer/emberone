/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_series_audience_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSeriesAudienceGroup {

    @EmbeddedId private AppointmentSeriesAudienceGroupId id;

    @MapsId("seriesId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private AppointmentSeries series;
}
