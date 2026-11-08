/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self surface of the optional AI assistance: which functions may be offered to this user. */
@RestController
@RequestMapping("/api/v1/self/ai")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class AiSelfController {

    private final AiCapabilitiesService capabilitiesService;

    @GetMapping("/capabilities")
    public ResponseEntity<AiCapabilitiesDto> getCapabilities() {
        return ResponseEntity.ok(capabilitiesService.capabilitiesForCurrentUser());
    }
}
