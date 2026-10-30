/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
        name = "appointment_responses",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "appointment_id", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AppointmentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "value", nullable = false)
    private AppointmentResponseValue value;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "responded_at", nullable = false)
    private Instant lastRespondedAt;

    @PrePersist
    protected void onCreate() {
        if (lastRespondedAt == null) {
            lastRespondedAt = Instant.now();
        }
    }
}
