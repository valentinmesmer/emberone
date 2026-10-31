/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userdocument.util;

import org.springframework.util.StringUtils;

/** Shared filename helpers for user-document uploads and system promotions. */
public final class UserDocumentFileNames {

    private UserDocumentFileNames() {}

    /**
     * Builds a safe display filename from a title, preserving the extension and falling back when
     * the title is blank or sanitizes to empty.
     */
    public static String displayFileName(String title, String extension, String fallbackFileName) {
        if (!StringUtils.hasText(title)) {
            return fallbackFileName;
        }
        String base =
                title.trim().replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").strip();
        if (base.isEmpty()) {
            return fallbackFileName;
        }
        return base + extension;
    }
}
