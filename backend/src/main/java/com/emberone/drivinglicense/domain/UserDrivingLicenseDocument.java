/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "driving_license_documents",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "driving_license_id", "document_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserDrivingLicenseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driving_license_id", nullable = false)
    private UserDrivingLicense license;

    @Column(name = "document_id", nullable = false)
    private Long documentId;
}
