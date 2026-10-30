/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.domain;

import com.emberone.organization.domain.Organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "appointment_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AppointmentSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "recurrence_rule", nullable = false)
    private String recurrenceRule;

    @Column(name = "series_start", nullable = false)
    private Instant seriesStart;

    @Column(name = "series_end")
    private Instant seriesEnd;

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "Europe/Berlin";

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AppointmentType type;

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

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppointmentSeriesAudienceGroup> audienceGroups = new ArrayList<>();

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
            AppointmentSeriesAudienceGroupId pk =
                    new AppointmentSeriesAudienceGroupId(tenant, null, gid);
            AppointmentSeriesAudienceGroup row =
                    AppointmentSeriesAudienceGroup.builder().id(pk).series(this).build();
            this.audienceGroups.add(row);
        }
    }
}
