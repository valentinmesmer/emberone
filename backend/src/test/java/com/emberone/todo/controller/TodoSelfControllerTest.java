/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.todo.dto.TodoDto;
import com.emberone.todo.dto.TodoPageDto;
import com.emberone.todo.list.TodoListFilter;
import com.emberone.todo.list.TodoSelfListFilterOptions;
import com.emberone.todo.service.TodoSelfService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TodoSelfControllerTest {

    @Mock TodoSelfService todoSelfService;
    @Mock TodoSelfListFilterOptions selfListFilterOptions;

    TodoSelfController controller;

    @BeforeEach
    void setUp() {
        controller = new TodoSelfController(todoSelfService, selfListFilterOptions);
    }

    @Test
    void listFilterOptionsDelegatesToSource() {
        ListFilterOptionsDto options = new ListFilterOptionsDto(Map.of());
        when(selfListFilterOptions.filterOptions()).thenReturn(options);

        ResponseEntity<ListFilterOptionsDto> response = controller.listFilterOptions();

        assertThat(response.getBody()).isEqualTo(options);
    }

    @Test
    void listTodosDelegatesToService() {
        TodoPageDto page =
                TodoPageDto.builder()
                        .content(List.of(TodoDto.builder().id(1L).build()))
                        .openCount(1)
                        .build();
        com.emberone.platform.web.list.ListQuery query =
                new com.emberone.platform.web.list.ListQuery(0, null, null, null);
        TodoListFilter filter = TodoListFilter.parse(List.of("OPEN"), List.of());
        when(todoSelfService.listTodos(query, filter)).thenReturn(page);

        ResponseEntity<TodoPageDto> response =
                controller.listTodos(query, List.of("OPEN"), List.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void getTodoDelegatesToService() {
        TodoDto dto = TodoDto.builder().id(9L).open(true).build();
        when(todoSelfService.getTodo(9L)).thenReturn(dto);

        ResponseEntity<TodoDto> response = controller.getTodo(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(todoSelfService).getTodo(9L);
    }

    @Test
    void completeDelegatesToService() {
        TodoDto dto = TodoDto.builder().id(5L).open(false).build();
        when(todoSelfService.complete(5L)).thenReturn(dto);

        ResponseEntity<TodoDto> response = controller.complete(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(todoSelfService).complete(5L);
    }

    @Test
    void getOpenCountDelegatesToService() {
        when(todoSelfService.countOpenTodos()).thenReturn(4L);

        ResponseEntity<java.util.Map<String, Long>> response = controller.getOpenCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("count", 4L));
    }
}
