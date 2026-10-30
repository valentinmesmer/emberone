/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
