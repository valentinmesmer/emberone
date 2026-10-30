/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SwaggerUiAliasControllerTest {

    private final SwaggerUiAliasController controller = new SwaggerUiAliasController();

    @Test
    void swaggerUiHtmlRedirectsToCanonicalPath() {
        assertThat(controller.swaggerUiHtml()).isEqualTo("redirect:/swagger-ui.html");
    }

    @Test
    void swaggerUiRootRedirectsToIndex() {
        assertThat(controller.swaggerUiRoot()).isEqualTo("redirect:/swagger-ui/index.html");
    }

    @Test
    void swaggerUiIndexRedirectsToCanonicalIndex() {
        assertThat(controller.swaggerUiIndex()).isEqualTo("redirect:/swagger-ui/index.html");
    }
}
