/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.app;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** General EmberOne application settings (shared across mail, links, integrations, etc.). */
@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.app")
public class EmberOneAppProperties {

    /**
     * Public URL of the EmberOne web app (e.g. https://app.example.com). Used for absolute links in
     * emails and can be reused elsewhere. Trailing slashes should be trimmed by consumers when
     * composing paths.
     */
    private String publicBaseUrl = "";
}
