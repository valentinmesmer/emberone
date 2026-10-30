/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.testsupport.SecurityWebMvcTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SwaggerUiAliasController.class)
@Import(SecurityWebMvcTestConfig.class)
class SwaggerUiAliasControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @Test
    void swaggerUiAliasIsPublicWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
