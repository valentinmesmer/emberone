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
@Table(name = "usr_entry_document_fulfillments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserEntryDocumentFulfillment {

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

    @Column(name = "template_sha256_hex", length = 64)
    private String templateSha256Hex;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    @Builder.Default
    private EntryDocumentFulfillmentOutcome outcome = EntryDocumentFulfillmentOutcome.ACCEPTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 32)
    private OnboardingDocumentFulfillmentMode mode;

    @Column(name = "user_document_id")
    private Long userDocumentId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isAcceptedForCurrentTemplate(int currentTemplateVersion) {
        return outcome == EntryDocumentFulfillmentOutcome.ACCEPTED
                && templateVersion == currentTemplateVersion;
    }

    public boolean isDeclinedForCurrentTemplate(int currentTemplateVersion) {
        return outcome == EntryDocumentFulfillmentOutcome.DECLINED
                && templateVersion == currentTemplateVersion;
    }
}
