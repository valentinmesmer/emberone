/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.realtime;

import com.emberone.event.domain.EventType;
import com.emberone.event.repository.OperationEventRepository;
import com.emberone.event.repository.ServiceEventRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventOrganizationResolver {

    private final ServiceEventRepository serviceEventRepository;
    private final OperationEventRepository operationEventRepository;

    public Optional<Long> resolveOrganizationId(EventType eventType, Long eventId) {
        if (eventType == null || eventId == null) {
            return Optional.empty();
        }
        return switch (eventType) {
            case SERVICE ->
                    serviceEventRepository
                            .findById(eventId)
                            .map(event -> event.getOrganization().getId());
            case OPERATION ->
                    operationEventRepository
                            .findById(eventId)
                            .map(event -> event.getOrganization().getId());
        };
    }
}
