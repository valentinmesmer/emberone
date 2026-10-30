/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.tenant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tenant_qualification_driving_licenses",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {
                        "tenant_id",
                        "qualification_definition_id",
                        "driving_license_config_key"
                    })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TenantQualificationDrivingLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qualification_definition_id", nullable = false)
    private TenantQualificationDefinition qualificationDefinition;

    @Column(name = "driving_license_config_key", nullable = false)
    private String drivingLicenseConfigKey;
}
