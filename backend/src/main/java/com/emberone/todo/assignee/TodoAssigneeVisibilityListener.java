/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.assignee;

import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.event.TodoAssigneeGroupsChangedEvent;
import com.emberone.todo.event.TodoVisibilityGainedEvent;
import com.emberone.todo.exception.TodoNotFoundException;
import com.emberone.todo.realtime.TodoRecipientResolver;
import com.emberone.todo.repository.TodoRepository;
import com.emberone.todo.type.TodoTypeDefinitionRegistry;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipActivatedEvent;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Translates membership joins and assignee-group remaps into {@link TodoVisibilityGainedEvent} for
 * notification and realtime adapters.
 */
@Service
@RequiredArgsConstructor
public class TodoAssigneeVisibilityListener {

    private final TodoRepository todoRepository;
    private final TodoTypeDefinitionRegistry typeDefinitions;
    private final TodoRecipientResolver recipientResolver;
    private final ApplicationEventPublisher eventPublisher;

    private static Set<Long> nullToEmpty(Set<Long> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    @AfterCommitEventListener
    public void onOrganizationGroupMembershipActivated(
            OrganizationGroupMembershipActivatedEvent event) {
        if (event.userId() == null || event.organizationGroupId() == null) {
            return;
        }
        List<Todo> openTodos =
                todoRepository.findOpenByOrganizationGroupId(event.organizationGroupId());
        for (Todo todo : openTodos) {
            if (!notifiesOnCreate(todo)) {
                continue;
            }
            Set<Long> recipients = recipientResolver.resolveRecipientUserIds(todo);
            if (!recipients.contains(event.userId())) {
                continue;
            }
            eventPublisher.publishEvent(
                    new TodoVisibilityGainedEvent(todo.getId(), Set.of(event.userId())));
        }
    }

    @AfterCommitEventListener
    public void onTodoAssigneeGroupsChanged(TodoAssigneeGroupsChangedEvent event) {
        if (event.todoId() == null) {
            return;
        }
        Set<Long> previous = nullToEmpty(event.previousOrganizationGroupIds());
        Set<Long> next = nullToEmpty(event.newOrganizationGroupIds());
        if (previous.equals(next)) {
            return;
        }
        Todo todo =
                todoRepository
                        .findById(event.todoId())
                        .orElseThrow(() -> new TodoNotFoundException(event.todoId()));
        if (!notifiesOnCreate(todo)) {
            return;
        }
        Set<Long> previousRecipients =
                recipientResolver.resolveGroupRecipientUserIds(todo, previous);
        Set<Long> currentRecipients = recipientResolver.resolveRecipientUserIds(todo);
        Set<Long> gained = new LinkedHashSet<>(currentRecipients);
        gained.removeAll(previousRecipients);
        if (CollectionUtils.isEmpty(gained)) {
            return;
        }
        eventPublisher.publishEvent(
                new TodoVisibilityGainedEvent(todo.getId(), Set.copyOf(gained)));
    }

    private boolean notifiesOnCreate(Todo todo) {
        return typeDefinitions.require(todo.getType()).notifiesOnCreate();
    }
}
