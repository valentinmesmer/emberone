/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.list;

import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Shared tag / content-type options for user-document list filter UIs. */
@Component
@RequiredArgsConstructor
public class UserDocumentListFilterOptions {

    private final UserDocumentService userDocumentService;

    public ListFilterOptionsDto forUser(long userId) {
        List<ListFilterOptionDto> tagOptions =
                userDocumentService.listDocumentTags(userId).stream()
                        .map(tag -> new ListFilterOptionDto(tag, tag))
                        .toList();
        List<ListFilterOptionDto> contentTypeOptions =
                userDocumentService.listDocumentContentTypes(userId).stream()
                        .map(type -> new ListFilterOptionDto(type, type))
                        .toList();
        return new ListFilterOptionsDto(
                Map.of("tag", tagOptions, "contentType", contentTypeOptions));
    }
}
