/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Time settings shared across API serialization and calendar semantics. */
@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.time")
public class OrganizationTimeProperties {

    /**
     * IANA timezone used to interpret organization wall-clock datetimes in API payloads (e.g.
     * {@code 2026-06-12T14:30:00}).
     */
    private String organizationZone = "Europe/Berlin";
}
