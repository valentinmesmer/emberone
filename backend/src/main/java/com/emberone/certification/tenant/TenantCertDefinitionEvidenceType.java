/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.tenant;

import com.emberone.certification.domain.CertEvidenceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tenant_cert_definition_evidence_types",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tenant_cert_def_evidence_types_unique",
                        columnNames = {"tenant_id", "tenant_cert_definition_id", "evidence_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TenantCertDefinitionEvidenceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tenant_cert_definition_id", nullable = false)
    private Long tenantCertDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private CertEvidenceType evidenceType;
}
