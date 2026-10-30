/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Exposes the same Swagger UI as springdoc under {@code /api/swagger-ui/...} for symmetry with
 * {@code /api/v1/**} REST routes.
 */
@Controller
public class SwaggerUiAliasController {

    private static final String CANONICAL_HTML = "redirect:/swagger-ui.html";
    private static final String CANONICAL_INDEX = "redirect:/swagger-ui/index.html";

    @GetMapping("/api/swagger-ui.html")
    public String swaggerUiHtml() {
        return CANONICAL_HTML;
    }

    @GetMapping({"/api/swagger-ui", "/api/swagger-ui/"})
    public String swaggerUiRoot() {
        return CANONICAL_INDEX;
    }

    @GetMapping("/api/swagger-ui/index.html")
    public String swaggerUiIndex() {
        return CANONICAL_INDEX;
    }
}
