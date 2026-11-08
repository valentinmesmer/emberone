/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Chat history kept by the browser; the server stores nothing. */
public record HelpChatRequest(
        @NotNull @Size(min = 1, max = HelpChatRequest.MAX_MESSAGES) List<@Valid @NotNull Message> messages) {

    public static final int MAX_MESSAGES = 10;
    public static final int MAX_CONTENT_LENGTH = 2000;

    public record Message(
            @NotNull @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = MAX_CONTENT_LENGTH) String content) {}
}
