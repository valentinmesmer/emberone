/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.domain;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.navigation.domain.MutableHasAppLink;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "todo_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Todo implements MutableHasAppLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private TodoType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb")
    private Map<String, Object> params;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = 64)
    private AppLinkType linkType;

    @Column(name = "link_id")
    private Long linkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TodoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_entity_type", length = 64)
    private TodoSourceEntityType sourceEntityType;

    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by_user_id")
    private Long completedByUserId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TodoAssigneeGroup> assigneeGroups = new ArrayList<>();

    /**
     * Users who must not see this todo (list, count, notifications), regardless of assignee rules.
     * Empty for most todos; e.g. workflow opener when self-approve is disabled.
     */
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TodoExcludedUser> excludedUsers = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = TodoStatus.OPEN;
        }
    }

    public boolean isOpen() {
        return status == TodoStatus.OPEN && archivedAt == null;
    }
}
