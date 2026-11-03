/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
