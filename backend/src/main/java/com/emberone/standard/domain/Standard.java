/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * A named, reusable bundle template (organization groups + permission groups + workflow definitions
 * + workflow approvals). The system ships a read-only {@link StandardSource#SYSTEM} baseline;
 * admins create {@link StandardSource#CUSTOM} standards by cloning. One custom standard is assigned
 * per organization type key via {@link StandardAssignment}; on organization creation the standard's
 * template items are materialized into per-org instance rows. The {@link StandardSource#SYSTEM}
 * baseline is a read-only clone source and must not be assigned directly.
 */
@Entity
@Table(
        name = "std_standards",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Standard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    @Builder.Default
    private StandardKind kind = StandardKind.ORGANIZATION_SETUP;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private StandardSource source;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /**
     * The standard this one was cloned from (the system baseline for custom standards); null for
     * the baseline itself.
     */
    @Column(name = "base_standard_id")
    private Long baseStandardId;

    public boolean isSystem() {
        return source == StandardSource.SYSTEM;
    }
}
