/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard: operation alarms ({@code operation_events}) across all organizations the user belongs
 * to vs. how many the user is marked present on ({@code event_attendances}, {@code OPERATION}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationAlarmAttendanceStatsDto {
    private long totalAlarms;
    private long attendedAlarms;
}
