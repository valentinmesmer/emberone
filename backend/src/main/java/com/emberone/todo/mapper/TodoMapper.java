/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.mapper;

import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoAssigneeGroup;
import com.emberone.todo.dto.TodoAssigneeGroupDto;
import com.emberone.todo.dto.TodoDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AppNavigationResolver.class)
public interface TodoMapper {

    @Mapping(target = "navigationPath", source = "entity", qualifiedByName = "navigationPath")
    @Mapping(
            target = "type",
            expression = "java(entity.getType() != null ? entity.getType().name() : null)")
    @Mapping(
            target = "status",
            expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    @Mapping(target = "open", expression = "java(entity.isOpen())")
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "body", ignore = true)
    @Mapping(target = "assigneeGroups", source = "entity.assigneeGroups")
    TodoDto toDto(Todo entity);

    default TodoAssigneeGroupDto toAssigneeGroupDto(TodoAssigneeGroup group) {
        if (group == null) {
            return null;
        }
        return TodoAssigneeGroupDto.builder()
                .organizationGroupId(group.getOrganizationGroupId())
                .build();
    }

    List<TodoAssigneeGroupDto> toAssigneeGroupDtoList(List<TodoAssigneeGroup> groups);
}
