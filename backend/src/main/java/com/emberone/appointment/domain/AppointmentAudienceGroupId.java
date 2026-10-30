/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AppointmentAudienceGroupId implements Serializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "organization_group_id", nullable = false)
    private Long organizationGroupId;
}
