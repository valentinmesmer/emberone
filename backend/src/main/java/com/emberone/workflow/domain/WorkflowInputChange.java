/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * One requested input adjustment on a workflow. A workflow may accumulate many over its lifetime;
 * only one may be {@link WorkflowInputChangeStatus#OPEN} at a time.
 */
@Entity
@Table(name = "wf_input_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInputChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkflowInputChangeStatus status;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (status == null) {
            status = WorkflowInputChangeStatus.OPEN;
        }
    }

    public boolean isOpen() {
        return status == WorkflowInputChangeStatus.OPEN;
    }

    public void fulfill(Instant at) {
        this.status = WorkflowInputChangeStatus.FULFILLED;
        this.fulfilledAt = at;
    }

    public void cancel(Instant at) {
        this.status = WorkflowInputChangeStatus.CANCELLED;
        this.fulfilledAt = at;
    }
}
