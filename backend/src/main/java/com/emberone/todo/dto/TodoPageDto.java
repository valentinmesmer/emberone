/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.dto;

import com.emberone.platform.web.PageDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoPageDto {

    private List<TodoDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private long openCount;

    public static TodoPageDto from(Page<?> page, List<TodoDto> content, long openCount) {
        PageDto<TodoDto> base = PageDto.from(page, content);
        return TodoPageDto.builder()
                .content(base.getContent())
                .page(base.getPage())
                .size(base.getSize())
                .totalElements(base.getTotalElements())
                .totalPages(base.getTotalPages())
                .hasNext(base.isHasNext())
                .openCount(openCount)
                .build();
    }
}
