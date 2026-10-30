/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.list;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Filter options for {@code GET /api/v1/self/documents}. */
@Component
@RequiredArgsConstructor
public class UserDocumentSelfListFilterOptions implements ListFilterOptionsSource {

    private final TokenService tokenService;
    private final UserDocumentListFilterOptions userDocumentListFilterOptions;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        return userDocumentListFilterOptions.forUser(tokenService.requireUserId());
    }
}
