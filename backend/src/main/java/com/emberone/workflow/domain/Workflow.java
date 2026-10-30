/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "wf_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "workflow_type_id", nullable = false)
    private Long workflowTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkflowStatus status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opened_by_user_id", nullable = false)
    private Long openedByUserId;

    @Column(name = "subject_entity_type", length = 64)
    private String subjectEntityType;

    @Column(name = "subject_entity_id")
    private Long subjectEntityId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * JSON array of stable open-concurrency keys; see {@link
     * com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys}.
     */
    @Column(name = "open_concurrency_keys", columnDefinition = "TEXT")
    private String openConcurrencyKeys;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Latest document promotion selection from the most recent approver (JSON). */
    @Column(name = "pending_document_promotions", columnDefinition = "TEXT")
    private String pendingDocumentPromotions;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 32)
    @Builder.Default
    private Set<WorkflowParticipant> participants = new LinkedHashSet<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 32)
    @Builder.Default
    private Set<WorkflowDecision> decisions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 32)
    @Builder.Default
    private Set<WorkflowInputChange> inputChanges = new LinkedHashSet<>();

    public Optional<WorkflowInputChange> findOpenInputChange() {
        return inputChanges.stream().filter(WorkflowInputChange::isOpen).findFirst();
    }
}
