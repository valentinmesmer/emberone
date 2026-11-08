/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.util.Base64;

/** Image input for vision models, sent as base64 data URL. Held in memory only. */
public record AiImage(String mediaType, byte[] data) {

    public String toDataUrl() {
        return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(data);
    }
}
