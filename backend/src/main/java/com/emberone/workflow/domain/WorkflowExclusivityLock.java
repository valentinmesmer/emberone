/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "wf_exclusivity_locks",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_wf_exclusivity_locks_tenant_lock_key",
                        columnNames = {"tenant_id", "lock_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExclusivityLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "lock_key", nullable = false, length = 255)
    private String lockKey;
}
