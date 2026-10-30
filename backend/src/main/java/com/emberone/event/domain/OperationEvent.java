/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "operation_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class OperationEvent extends BaseEvent {

    @Column(name = "number")
    private String number;

    @Column(name = "keyword")
    private String keyword;

    @Column(name = "tag")
    private String tag;

    @Column(name = "special_signal", nullable = false)
    private Boolean specialSignal;

    @Override
    public EventType getEventType() {
        return EventType.OPERATION;
    }
}
