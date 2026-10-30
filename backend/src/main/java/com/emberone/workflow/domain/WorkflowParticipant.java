/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
        name = "wf_participants",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "workflow_id", "slot_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement", nullable = false, length = 32)
    private ParticipantRequirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 32)
    private PrincipalType principalType;

    @Column(name = "principal_id")
    private Long principalId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "slot_key", nullable = false)
    private String slotKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_status", length = 32)
    private ParticipantSlotStatus slotStatus;

    @Column(name = "claimed_by_user_id")
    private Long claimedByUserId;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    public boolean isApprover() {
        return role == ParticipantRole.APPROVER;
    }
}
