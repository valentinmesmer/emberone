/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.domain;

import com.emberone.organization.domain.Organization;
import com.emberone.platform.domain.LifecycleFields;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "organization_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrganizationGroup implements LifecycleFields.Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private OrganizationGroupType type = OrganizationGroupType.CUSTOM;

    /**
     * Stable key linking this instance row to the standard template it was materialized from; null
     * for ad-hoc org-only groups.
     */
    @Column(name = "config_key")
    private String configKey;

    /**
     * Id of the {@code std_org_group_templates} item this row was materialized from; null for
     * archived rows or rows no longer driven by the standard.
     */
    @Column(name = "standard_origin_id")
    private Long standardOriginId;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
