/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "usr_entry_document_fulfillment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserEntryDocumentFulfillmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(name = "template_sha256_hex", nullable = false, length = 64)
    private String templateSha256Hex;

    @Column(name = "confirmation_text", nullable = false)
    private String confirmationText;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private EntryDocumentFulfillmentOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private EntryDocumentFulfillmentChannel channel;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "admin_record_reason")
    private String adminRecordReason;

    @Column(name = "workflow_reviewer_user_id")
    private Long workflowReviewerUserId;

    @Column(name = "workflow_reviewed_at")
    private Instant workflowReviewedAt;

    @Column(name = "user_document_id")
    private Long userDocumentId;
}
