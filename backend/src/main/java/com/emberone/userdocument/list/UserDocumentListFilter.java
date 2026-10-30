/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.list;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

public record UserDocumentListFilter(
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) UserDocumentListViewed viewed,
        @RequestParam(required = false) String contentType) {

    public UserDocumentListFilter {
        if (viewed == null) {
            viewed = UserDocumentListViewed.ALL;
        }
        tag = normalizeOptionalText(tag);
        contentType = normalizeOptionalText(contentType);
    }

    private static String normalizeOptionalText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }
}
