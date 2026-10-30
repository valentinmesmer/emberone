/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import java.time.Instant;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class OrganizationWallClockInstantSerializer extends ValueSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext context) {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(OrganizationWallClockTime.format(value));
    }
}
