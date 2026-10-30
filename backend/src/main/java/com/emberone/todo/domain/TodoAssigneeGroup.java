/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "todo_assignee_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TodoAssigneeGroupId.class)
public class TodoAssigneeGroup {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Id
    @Column(name = "todo_id", nullable = false)
    private Long todoId;

    @Id
    @Column(name = "organization_group_id", nullable = false)
    private Long organizationGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", insertable = false, updatable = false)
    private Todo todo;
}
