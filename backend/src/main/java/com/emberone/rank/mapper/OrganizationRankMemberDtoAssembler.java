/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.mapper;

import com.emberone.rank.dto.OrganizationRankMemberDto;
import com.emberone.rank.dto.OrganizationRankMemberPageDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class OrganizationRankMemberDtoAssembler {

    public OrganizationRankMemberPageDto toPageDto(Page<OrganizationRankMemberDto> page) {
        return OrganizationRankMemberPageDto.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
