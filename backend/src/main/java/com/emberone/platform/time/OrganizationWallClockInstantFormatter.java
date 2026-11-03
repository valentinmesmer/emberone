/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
