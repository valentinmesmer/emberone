/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Converts between organization wall-clock strings ({@code yyyy-MM-dd'T'HH:mm:ss}) and UTC {@link
 * Instant}.
 */
public final class OrganizationWallClockTime {

    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private static volatile ZoneId organizationZone = ZoneId.of("Europe/Berlin");

    private OrganizationWallClockTime() {}

    public static void configure(ZoneId zoneId) {
        organizationZone = zoneId;
    }

    public static ZoneId organizationZone() {
        return organizationZone;
    }

    /** Calendar day in the organization zone (for date-only business rules). */
    public static LocalDate today() {
        return LocalDate.now(organizationZone);
    }

    /** Calendar day of an instant in the organization zone. */
    public static LocalDate toOrganizationDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(organizationZone).toLocalDate();
    }

    public static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value, FORMATTER);
            return localDateTime.atZone(organizationZone).toInstant();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid organization wall-clock datetime: " + value, ex);
        }
    }

    public static String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return FORMATTER.format(instant.atZone(organizationZone));
    }
}
