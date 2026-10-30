/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "cert_event_grant_recipients",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_cert_event_grant_recipients_unique",
                        columnNames = {"tenant_id", "grant_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CertificationEventGrantRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "grant_id", nullable = false)
    private Long grantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
