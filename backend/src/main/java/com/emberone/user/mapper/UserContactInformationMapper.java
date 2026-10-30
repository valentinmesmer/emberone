/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.mapper;

import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.dto.UserContactInformationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserContactInformationMapper {

    @Mapping(
            target = "userId",
            expression =
                    "java(contactInformation.getUser() != null ? contactInformation.getUser().getId() : null)")
    UserContactInformationDto toDto(UserContactInformation contactInformation);
}
