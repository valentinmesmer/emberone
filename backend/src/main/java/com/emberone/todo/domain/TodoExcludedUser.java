/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "todo_excluded_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TodoExcludedUserId.class)
public class TodoExcludedUser {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Id
    @Column(name = "todo_id", nullable = false)
    private Long todoId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", insertable = false, updatable = false)
    private Todo todo;
}
