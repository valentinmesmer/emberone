/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment;

import java.util.Set;

/**
 * Shared allowlist for user-uploaded binary attachments that may be opened inline (PDF and raster
 * images only - no SVG/HTML).
 */
public final class AllowedAttachmentMediaTypes {

    public static final Set<String> IMAGE_VALUES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    public static final Set<String> VALUES =
            Set.of("application/pdf", "image/jpeg", "image/png", "image/gif", "image/webp");

    private AllowedAttachmentMediaTypes() {}

    public static boolean isAllowed(String contentType) {
        return contentType != null
                && !contentType.isBlank()
                && VALUES.contains(contentType.toLowerCase());
    }

    public static boolean isAllowedImage(String contentType) {
        return contentType != null
                && !contentType.isBlank()
                && IMAGE_VALUES.contains(contentType.toLowerCase());
    }

    public static String forDelivery(String storedContentType) {
        return isAllowed(storedContentType)
                ? storedContentType.toLowerCase()
                : "application/octet-stream";
    }
}
