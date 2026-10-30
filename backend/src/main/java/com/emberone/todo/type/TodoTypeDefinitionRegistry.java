/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.type;

import com.emberone.todo.domain.TodoType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TodoTypeDefinitionRegistry {

    private final Map<TodoType, TodoTypeDefinition> definitions;

    public TodoTypeDefinitionRegistry(List<TodoTypeDefinition> definitionList) {
        Map<TodoType, TodoTypeDefinition> map = new EnumMap<>(TodoType.class);
        for (TodoTypeDefinition definition : definitionList) {
            map.put(definition.type(), definition);
        }
        this.definitions = Map.copyOf(map);
    }

    public TodoTypeDefinition require(TodoType type) {
        TodoTypeDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalStateException("No todo type definition for " + type);
        }
        return definition;
    }
}
