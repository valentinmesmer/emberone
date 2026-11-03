/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.http;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;

public final class ContentDispositionHeaders {

    private ContentDispositionHeaders() {}

    public static String attachment(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
