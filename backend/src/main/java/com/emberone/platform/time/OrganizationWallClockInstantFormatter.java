/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import java.time.Instant;
import java.util.Locale;
import org.springframework.format.Formatter;

public class OrganizationWallClockInstantFormatter implements Formatter<Instant> {

    @Override
    public Instant parse(String text, Locale locale) {
        return OrganizationWallClockTime.parse(text);
    }

    @Override
    public String print(Instant instant, Locale locale) {
        return OrganizationWallClockTime.format(instant);
    }
}
