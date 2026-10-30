/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.tenant;

import com.emberone.platform.domain.LifecycleFields;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Tenant certification catalog entry (Mandant Einstellungen). Seeded once from the tenant baseline
 * or created by admins; selected into org standards via {@code config_key}.
 */
@Entity
@Table(
        name = "tenant_cert_definitions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "config_key"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TenantCertDefinition implements LifecycleFields.ActivatableCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "reminder_days_before_expiry", nullable = false)
    @Builder.Default
    private Integer reminderDaysBeforeExpiry = 30;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
