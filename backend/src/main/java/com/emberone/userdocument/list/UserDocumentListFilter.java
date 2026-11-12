/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userdocument.list;

import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

public record UserDocumentListFilter(
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) UserDocumentListViewed viewed,
        @RequestParam(required = false) String contentType,
        @RequestParam(required = false) List<Long> ids) {

    public UserDocumentListFilter {
        if (viewed == null) {
            viewed = UserDocumentListViewed.ALL;
        }
        tag = normalizeOptionalText(tag);
        contentType = normalizeOptionalText(contentType);
        ids = ids == null || ids.isEmpty() ? null : List.copyOf(ids);
    }

    private static String normalizeOptionalText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }
}
