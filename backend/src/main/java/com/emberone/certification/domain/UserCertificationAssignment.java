/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

import com.emberone.platform.navigation.domain.AppLinkType;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

/** Assigns a tenant certification definition (Einzelnachweis) to a user. */
@Entity
@Table(name = "cert_user_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserCertificationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "responsible_organization_id", nullable = false)
    private Long responsibleOrganizationId;

    @Column(name = "tenant_cert_definition_id", nullable = false)
    private Long tenantCertDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    @Builder.Default
    private CertificationAssignmentSource source = CertificationAssignmentSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_app_link_type")
    private AppLinkType sourceAppLinkType;

    @Column(name = "source_app_link_id")
    private Long sourceAppLinkId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public boolean isActiveOn(LocalDate date) {
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }
}
