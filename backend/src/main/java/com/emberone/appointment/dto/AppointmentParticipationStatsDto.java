/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentParticipationStatsDto {
    private int yesCount;
    private int noCount;
    private int uncertainCount;
    private List<AppointmentParticipantEntryDto> yesParticipants;
    private List<AppointmentParticipantEntryDto> noParticipants;
    private List<AppointmentParticipantEntryDto> uncertainParticipants;
}
