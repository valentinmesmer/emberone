/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
        name = "idm_users",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_identity_users_emberone",
                        columnNames = {"tenant_id", "emberone_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class IdentityUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "emberone_user_id", nullable = false)
    private Long emberoneUserId;

    @Column(name = "app_role", nullable = false, length = 32)
    private String appRole;

    @Column(name = "idp_user_id", unique = true)
    private String idpUserId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "private_email", nullable = false)
    private String privateEmail;

    @Column(name = "account_enabled", nullable = false)
    private Boolean accountEnabled;

    @Column(name = "deprovision_pending_at")
    private Instant deprovisionPendingAt;

    @Column(name = "deprovision_attempts", nullable = false)
    @Builder.Default
    private int deprovisionAttempts = 0;

    @Column(name = "deprovision_last_error", columnDefinition = "TEXT")
    private String deprovisionLastError;

    @Column(name = "deprovision_next_attempt_at")
    private Instant deprovisionNextAttemptAt;

    @Column(name = "deprovision_processing_started_at")
    private Instant deprovisionProcessingStartedAt;
}
