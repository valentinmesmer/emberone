/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.controller;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.todo.dto.TodoDto;
import com.emberone.todo.dto.TodoPageDto;
import com.emberone.todo.list.TodoListFilter;
import com.emberone.todo.list.TodoSelfListFilterOptions;
import com.emberone.todo.service.TodoSelfService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/self/todos")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class TodoSelfController {

    private final TodoSelfService todoSelfService;
    private final TodoSelfListFilterOptions selfListFilterOptions;

    @GetMapping("/list-filter-options")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(selfListFilterOptions.filterOptions());
    }

    @GetMapping
    public ResponseEntity<TodoPageDto> listTodos(
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        return ResponseEntity.ok(
                todoSelfService.listTodos(query, TodoListFilter.parse(status, type)));
    }

    @GetMapping("/open-count")
    public ResponseEntity<Map<String, Long>> getOpenCount() {
        return ResponseEntity.ok(Map.of("count", todoSelfService.countOpenTodos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoDto> getTodo(@PathVariable Long id) {
        return ResponseEntity.ok(todoSelfService.getTodo(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<TodoDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(todoSelfService.complete(id));
    }
}
