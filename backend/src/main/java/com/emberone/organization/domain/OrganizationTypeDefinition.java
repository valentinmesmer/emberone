/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tenant-defined kind of organization, referenced from {@link Organization} and {@link
 * com.emberone.standard.domain.StandardAssignment}.
 */
@Entity
@Table(name = "organization_type_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrganizationTypeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "config_key")
    private String configKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
