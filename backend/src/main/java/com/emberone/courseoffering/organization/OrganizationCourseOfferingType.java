/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.organization;

import jakarta.persistence.*;
import lombok.*;

/** A training course type an organization may offer, referenced by tenant catalog config key. */
@Entity
@Table(
        name = "org_course_offering_types",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {"tenant_id", "organization_id", "definition_config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationCourseOfferingType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "definition_config_key", nullable = false)
    private String definitionConfigKey;
}
