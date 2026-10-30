/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.mail")
public class EmberOneMailProperties {

    private Smtp smtp = new Smtp();
    private String fromAddress = "";
    private String fromName = "";

    @Getter
    @Setter
    public static class Smtp {
        private String host = "";
        private int port = 587;
        private String username = "";
        private String password = "";
        private boolean startTls = true;
        private boolean auth = true;
    }
}
