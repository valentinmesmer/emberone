/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.mapper;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.dto.TodoAssigneeGroupDto;
import com.emberone.todo.dto.TodoDto;
import com.emberone.todo.dto.TodoPageDto;
import com.emberone.todo.i18n.TodoMessageResolver;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TodoDtoAssembler {

    private final TodoMapper todoMapper;
    private final TodoMessageResolver messageResolver;
    private final TenantLocaleService tenantLocaleService;

    private static String stringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    public TodoDto toDto(Todo todo) {
        TodoDto dto = todoMapper.toDto(todo);
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(todo.getTenantId());
        dto.setTitle(messageResolver.resolveTitle(todo, locale));
        dto.setBody(messageResolver.resolveBody(todo, locale));
        enrichAssigneeGroupNames(dto, todo.getParams());
        return dto;
    }

    public TodoPageDto toPageDto(Page<Todo> page, long openCount) {
        List<TodoDto> content = page.getContent().stream().map(this::toDto).toList();
        return TodoPageDto.from(page, content, openCount);
    }

    private void enrichAssigneeGroupNames(TodoDto dto, Map<String, Object> params) {
        List<TodoAssigneeGroupDto> groups = dto.getAssigneeGroups();
        if (groups == null || groups.isEmpty()) {
            return;
        }
        String groupName =
                params != null ? stringParam(params, TodoParamKeys.ORGANIZATION_GROUP_NAME) : null;
        if (!StringUtils.hasText(groupName)) {
            return;
        }
        for (TodoAssigneeGroupDto group : groups) {
            if (!StringUtils.hasText(group.getOrganizationGroupName())) {
                group.setOrganizationGroupName(groupName);
            }
        }
    }
}
