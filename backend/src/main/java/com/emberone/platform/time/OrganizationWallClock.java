/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Marks an {@link java.time.Instant} API field as organization wall-clock time ({@value
 * OrganizationWallClockTime#PATTERN}) in {@link OrganizationWallClockTime#organizationZone()}.
 *
 * <p>This is always the single, globally configured organization zone, even for fields on
 * appointment series that were expanded using a different per-series recurrence timezone (see
 * {@code AppointmentRecurrenceService}). The recurrence timezone only affects how occurrence times
 * are computed (DST-aware); it does not change how they are displayed.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = OrganizationWallClockInstantSerializer.class)
@JsonDeserialize(using = OrganizationWallClockInstantDeserializer.class)
public @interface OrganizationWallClock {}
