/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.time;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.platform.config.EmberOneJsonMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OrganizationWallClockTimeTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @BeforeEach
    void setUp() {
        OrganizationWallClockTime.configure(BERLIN);
    }

    @Test
    void toOrganizationDateUsesOrganizationZone() {
        Instant instant = LocalDateTime.of(2026, 8, 12, 22, 0).atZone(ZoneOffset.UTC).toInstant();

        assertThat(OrganizationWallClockTime.toOrganizationDate(instant))
                .isEqualTo(LocalDate.of(2026, 8, 13));
    }

    @Test
    void parseAndFormatRoundTripDuringCest() {
        Instant instant = OrganizationWallClockTime.parse("2026-06-12T14:30:00");

        assertThat(instant)
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 14, 30).atZone(BERLIN).toInstant());
        assertThat(OrganizationWallClockTime.format(instant)).isEqualTo("2026-06-12T14:30:00");
    }

    @Test
    void parseAndFormatRoundTripDuringCet() {
        Instant instant = OrganizationWallClockTime.parse("2026-01-12T14:30:00");

        assertThat(instant)
                .isEqualTo(LocalDateTime.of(2026, 1, 12, 14, 30).atZone(BERLIN).toInstant());
        assertThat(OrganizationWallClockTime.format(instant)).isEqualTo("2026-01-12T14:30:00");
    }

    @Test
    void jsonMapperSerializesAndDeserializesOrganizationWallClockFields() throws Exception {
        JsonMapper mapper = EmberOneJsonMapper.create();
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setStart(OrganizationWallClockTime.parse("2026-06-12T14:30:00"));
        request.setEnd(OrganizationWallClockTime.parse("2026-06-12T16:30:00"));

        String json = mapper.writeValueAsString(request);
        CreateAppointmentRequest parsed = mapper.readValue(json, CreateAppointmentRequest.class);

        assertThat(json).contains("\"start\":\"2026-06-12T14:30:00\"");
        assertThat(json).contains("\"end\":\"2026-06-12T16:30:00\"");
        assertThat(parsed.getStart()).isEqualTo(request.getStart());
        assertThat(parsed.getEnd()).isEqualTo(request.getEnd());
        assertThat(parsed.getStart())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 14, 30).atZone(BERLIN).toInstant());
    }

    @Test
    void jsonMapperSerializesTimeEntryWallClockFields() throws Exception {
        JsonMapper mapper = EmberOneJsonMapper.create();
        com.emberone.timeentry.dto.CreateUserTimeEntryRequest request =
                com.emberone.timeentry.dto.CreateUserTimeEntryRequest.builder()
                        .organizationId(1L)
                        .startTime(OrganizationWallClockTime.parse("2026-06-12T08:00:00"))
                        .endTime(OrganizationWallClockTime.parse("2026-06-12T10:00:00"))
                        .build();

        String json = mapper.writeValueAsString(request);
        com.emberone.timeentry.dto.CreateUserTimeEntryRequest parsed =
                mapper.readValue(json, com.emberone.timeentry.dto.CreateUserTimeEntryRequest.class);

        assertThat(json).contains("\"startTime\":\"2026-06-12T08:00:00\"");
        assertThat(parsed.getStartTime()).isEqualTo(request.getStartTime());
        assertThat(parsed.getEndTime()).isEqualTo(request.getEndTime());
    }

    @Test
    void storedInstantIsRealUtcNotWallClockAsUtc() {
        Instant parsed = OrganizationWallClockTime.parse("2026-06-12T14:30:00");
        Instant naiveUtc = LocalDateTime.of(2026, 6, 12, 14, 30).toInstant(ZoneOffset.UTC);

        assertThat(parsed).isNotEqualTo(naiveUtc);
        assertThat(parsed).isEqualTo(naiveUtc.minusSeconds(2 * 3600));
    }
}
