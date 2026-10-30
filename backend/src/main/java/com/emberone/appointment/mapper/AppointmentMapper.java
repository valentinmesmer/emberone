/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.mapper;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.appointment.dto.AppointmentAudienceGroupDto;
import com.emberone.appointment.dto.AppointmentDto;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {

    private final OrganizationGroupRepository organizationGroupRepository;

    private static List<Long> detachGroupIds(List<Long> groupIds) {
        if (groupIds == null) {
            return List.of();
        }
        List<Long> sortedGroupIds = new ArrayList<>(groupIds);
        sortedGroupIds.sort(Long::compareTo);
        return sortedGroupIds;
    }

    public AppointmentDto toDto(Appointment appointment) {
        AppointmentDto dto =
                AppointmentDto.builder()
                        .id(appointment.getId())
                        .organizationId(appointment.getOrganization().getId())
                        .seriesId(
                                appointment.getSeries() != null
                                        ? appointment.getSeries().getId()
                                        : null)
                        .recurrenceRule(
                                appointment.getSeries() != null
                                        ? appointment.getSeries().getRecurrenceRule()
                                        : null)
                        .seriesEnd(
                                appointment.getSeries() != null
                                        ? appointment.getSeries().getSeriesEnd()
                                        : null)
                        .recurrenceTimezone(
                                appointment.getSeries() != null
                                        ? appointment.getSeries().getTimezone()
                                        : null)
                        .occurrenceStart(appointment.getOccurrenceStart())
                        .name(appointment.getName())
                        .type(appointment.getType())
                        .start(appointment.getStart())
                        .end(appointment.getEnd())
                        .location(appointment.getLocation())
                        .description(appointment.getDescription())
                        .responseMode(appointment.getResponseMode())
                        .responseDeadlineMinutesBefore(
                                appointment.getResponseDeadlineMinutesBefore())
                        .audienceScope(appointment.getAudienceScope())
                        .audienceGroupIds(detachGroupIds(appointment.getAudienceGroupIds()))
                        .isException(appointment.getIsException())
                        .archived(appointment.isArchived())
                        .build();
        enrichAudienceGroups(dto);
        return dto;
    }

    private void enrichAudienceGroups(AppointmentDto dto) {
        if (dto.getAudienceScope() != AudienceScope.SELECTED_GROUPS) {
            return;
        }
        List<Long> groupIds = dto.getAudienceGroupIds();
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        List<AppointmentAudienceGroupDto> audienceGroups =
                organizationGroupRepository.findAllById(groupIds).stream()
                        .sorted(
                                Comparator.comparing(
                                                OrganizationGroup::getName,
                                                String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(OrganizationGroup::getId))
                        .map(
                                group ->
                                        AppointmentAudienceGroupDto.builder()
                                                .id(group.getId())
                                                .name(group.getName())
                                                .build())
                        .toList();
        dto.setAudienceGroups(audienceGroups);
    }
}
