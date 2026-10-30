/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.mapper;

import com.emberone.absence.domain.UserAbsence;
import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAbsenceDtoAssembler {

    private final UserAbsenceMapper userAbsenceMapper;

    public UserAbsenceDto toDto(UserAbsence absence) {
        return userAbsenceMapper.toDto(absence);
    }

    public UserAbsencePageDto toPageDto(Page<UserAbsence> page) {
        return UserAbsencePageDto.builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
