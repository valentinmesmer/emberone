/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

import com.emberone.certification.domain.CertGrantRecipientMode;
import com.emberone.event.domain.EventType;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
        name = "cert_event_grants",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_cert_event_grants_pair",
                        columnNames = {
                            "tenant_id",
                            "event_type",
                            "event_id",
                            "tenant_cert_definition_id"
                        }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CertificationEventGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "tenant_cert_definition_id", nullable = false)
    private Long tenantCertDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_mode", nullable = false)
    private CertGrantRecipientMode recipientMode;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
