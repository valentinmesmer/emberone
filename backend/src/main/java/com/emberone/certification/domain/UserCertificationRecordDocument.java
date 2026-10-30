/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

import jakarta.persistence.*;
import lombok.*;

/** Links a certification record to one or more personal documents (proof files). */
@Entity
@Table(
        name = "cert_user_record_documents",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_cert_user_record_documents_unique",
                        columnNames = {"tenant_id", "record_id", "document_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserCertificationRecordDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private UserCertificationRecord record;

    @Column(name = "document_id", nullable = false)
    private Long documentId;
}
