/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.realtime;

import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.dto.TodoDto;
import com.emberone.todo.mapper.TodoDtoAssembler;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class TodoRealtimeNotifier {

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;
    private final TodoDtoAssembler todoDtoAssembler;
    private final TodoRecipientResolver recipientResolver;

    public void notifyCreated(Todo todo) {
        notify(todo, "todo.created", recipientResolver.resolveRecipientUserIds(todo));
    }

    public void notifyCreated(Todo todo, Set<Long> recipientUserIds) {
        notify(todo, "todo.created", recipientUserIds);
    }

    public void notifyUpdated(Todo todo) {
        notify(todo, "todo.updated", recipientResolver.resolveRecipientUserIds(todo));
    }

    private void notify(Todo todo, String eventType, Set<Long> recipients) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null || todo.getId() == null || CollectionUtils.isEmpty(recipients)) {
            return;
        }
        TodoDto dto = todoDtoAssembler.toDto(todo);
        for (Long userId : recipients) {
            publisher.publishToUser(
                    todo.getTenantId(),
                    userId,
                    RealtimePublisher.TODOS_QUEUE,
                    RealtimeEvent.withPayload(eventType, Map.of("id", todo.getId()), dto));
        }
    }
}
