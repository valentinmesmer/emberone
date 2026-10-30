/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.domain;

import com.emberone.organization.domain.OrganizationTypeDefinition;
import jakarta.persistence.*;
import lombok.*;

/**
 * Selects exactly one {@link Standard} for an {@link OrganizationTypeDefinition}. When an
 * organization of that type is created, the assigned standard's templates are materialized into
 * per-org instance rows.
 */
@Entity
@Table(
        name = "std_assignments",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "organization_type_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class StandardAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_type_id", nullable = false)
    private OrganizationTypeDefinition organizationType;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;
}
