/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.mapper;

import com.emberone.timeentry.domain.UserTimeEntry;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserTimeEntryDtoAssembler {

    private final UserTimeEntryMapper mapper;

    public UserTimeEntryDto toDto(UserTimeEntry entity) {
        return mapper.toDto(entity);
    }

    public UserTimeEntryPageDto toPageDto(Page<UserTimeEntry> page) {
        return UserTimeEntryPageDto.builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
