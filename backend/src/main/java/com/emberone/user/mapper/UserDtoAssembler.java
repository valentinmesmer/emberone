/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.mapper;

import com.emberone.platform.web.PageDto;
import com.emberone.user.dto.UserDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class UserDtoAssembler {

    public PageDto<UserDto> toPageDto(List<UserDto> content, Page<?> page) {
        return PageDto.from(page, content);
    }
}
