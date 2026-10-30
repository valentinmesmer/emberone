/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.domain;

import com.emberone.organization.domain.Organization;
import com.emberone.platform.domain.LifecycleFields;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(
        name = "appointments",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_appointments_series_occurrence",
                        columnNames = {"tenant_id", "series_id", "occurrence_start"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Appointment implements LifecycleFields.Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private AppointmentSeries series;

    @Column(name = "occurrence_start", nullable = false)
    private Instant occurrenceStart;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AppointmentType type;

    @Column(name = "starts_at", nullable = false)
    private Instant start;

    @Column(name = "ends_at", nullable = false)
    private Instant end;

    @Column(name = "location")
    private String location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_mode", nullable = false)
    private AppointmentResponseMode responseMode;

    @Column(name = "response_deadline_minutes_before", nullable = false)
    @Builder.Default
    private Integer responseDeadlineMinutesBefore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_scope", nullable = false)
    private AudienceScope audienceScope;

    @Column(name = "is_exception", nullable = false)
    @Builder.Default
    private Boolean isException = false;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<AppointmentAudienceGroup> audienceGroups = new ArrayList<>();

    public List<Long> getAudienceGroupIds() {
        if (audienceGroups == null || audienceGroups.isEmpty()) {
            return new ArrayList<>();
        }
        return audienceGroups.stream().map(g -> g.getId().getOrganizationGroupId()).toList();
    }

    public void setAudienceGroupIds(List<Long> groupIds) {
        replaceAudienceGroupIds(groupIds);
    }

    public void replaceAudienceGroupIds(List<Long> groupIds) {
        if (this.audienceGroups == null) {
            this.audienceGroups = new ArrayList<>();
        }
        this.audienceGroups.clear();
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        Long tenant =
                this.tenantId != null
                        ? this.tenantId
                        : (this.organization != null ? this.organization.getTenantId() : null);
        for (Long gid : groupIds) {
            AppointmentAudienceGroupId pk = new AppointmentAudienceGroupId(tenant, null, gid);
            AppointmentAudienceGroup row =
                    AppointmentAudienceGroup.builder().id(pk).appointment(this).build();
            this.audienceGroups.add(row);
        }
    }
}
