/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

import com.emberone.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * A user's registration for a course offering. Created when the registration workflow opens and
 * kept in sync with it; the seat/waitlist decision happens on approval under a row lock on the
 * offering (see {@code CourseRegistrationService}).
 */
@Entity
@Table(name = "course_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Read-only view of {@link #userId} for searching and sorting by name. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /** The registration workflow; cleared when the workflow is purged by retention. */
    @Column(name = "workflow_id")
    private Long workflowId;

    /**
     * The open cancellation workflow of a participant, cleared when it is rejected or cancelled and
     * kept as history once it was approved. A participant with this set cannot start a second
     * cancellation.
     */
    @Column(name = "cancellation_workflow_id")
    private Long cancellationWorkflowId;

    /**
     * The audience entry whose quota this participant or waitlist place belongs to; set only for
     * offerings with quotas, kept as history after the registration ended.
     */
    @Column(name = "audience_id")
    private Long audienceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CourseRegistrationStatus status;

    /** The user already held the offered course when registering. */
    @Column(name = "already_held", nullable = false)
    private boolean alreadyHeld;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    /** Approval time; orders the waitlist (oldest first). */
    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ended_by")
    private CourseRegistrationEndedBy endedBy;

    @Column(name = "ended_by_user_id")
    private Long endedByUserId;

    /** Mandatory reason when the organization ended the registration. */
    @Column(name = "end_reason", columnDefinition = "TEXT")
    private String endReason;

    /**
     * Final result decided by the organization ({@code true}: passed and credited on completion).
     * Null while undecided, see {@code CourseCompletionService}.
     */
    @Column(name = "final_passed")
    private Boolean finalPassed;

    /** Who set {@link #finalPassed} and when; null when it was never set explicitly. */
    @Column(name = "final_decided_by_user_id")
    private Long finalDecidedByUserId;

    @Column(name = "final_decided_at")
    private Instant finalDecidedAt;
}
