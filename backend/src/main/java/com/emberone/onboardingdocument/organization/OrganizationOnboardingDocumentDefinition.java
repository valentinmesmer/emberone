/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.organization;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.platform.domain.LifecycleFields;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/** Organization-specific onboarding entry document definition. */
@Entity
@Table(
        name = "onb_org_document_definitions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_onb_org_document_definitions",
                    columnNames = {"tenant_id", "organization_id", "config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrganizationOnboardingDocumentDefinition
        implements LifecycleFields.ActivatableCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_mode", nullable = false)
    private OnboardingDocumentFulfillmentMode fulfillmentMode;

    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    private boolean mandatory = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /** Current template version number; {@code 0} means no active template. */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 0;
}
