/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

import com.emberone.platform.navigation.domain.AppLinkType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/**
 * A concrete proof that a user completed a certification (definition). Origin is always {@code
 * appLinkType}/{@code appLinkId} when present; {@code submissionMethod} records how the proof was
 * created.
 *
 * <ul>
 *   <li>{@link CertificationSubmissionMethod#MANUAL} - no app link
 *   <li>{@link CertificationSubmissionMethod#WORKFLOW} - {@code WORKFLOW} + workflow id
 *   <li>{@link CertificationSubmissionMethod#EVENT} - {@code CERTIFICATION_EVENT_GRANT} + grant id
 *   <li>{@link CertificationSubmissionMethod#ONBOARDING} - {@code ONBOARDING_INVITATION} +
 *       invitation id
 * </ul>
 *
 * {@code expiryDate} null = never expires.
 */
@Entity
@Table(name = "cert_user_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserCertificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_cert_definition_id", nullable = false)
    private Long tenantCertDefinitionId;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<UserCertificationRecordDocument> documents = new ArrayList<>();

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_link_type")
    private AppLinkType appLinkType;

    @Column(name = "app_link_id")
    private Long appLinkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_method", nullable = false)
    @Builder.Default
    private CertificationSubmissionMethod submissionMethod = CertificationSubmissionMethod.MANUAL;
}
