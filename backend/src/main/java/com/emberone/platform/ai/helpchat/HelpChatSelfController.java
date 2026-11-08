/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self surface of the AI help chat. Uses the authenticated user's context only. */
@RestController
@RequestMapping("/api/v1/self/ai/help-chat")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class HelpChatSelfController {

    private final HelpChatService helpChatService;

    @PostMapping
    public ResponseEntity<HelpChatResponse> chat(@Valid @RequestBody HelpChatRequest request) {
        return ResponseEntity.ok(helpChatService.chat(request));
    }
}
