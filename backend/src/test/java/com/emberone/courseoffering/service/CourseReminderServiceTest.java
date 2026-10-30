/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.config.CourseOfferingProperties;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseReminderServiceTest {

    private static final long TENANT_ID = 7L;

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseReminderProcessor processor;

    private CourseOfferingProperties properties;
    private CourseReminderService service;

    @BeforeEach
    void setUp() {
        properties = new CourseOfferingProperties();
        service = new CourseReminderService(offeringRepository, processor, properties);
    }

    @Test
    void remindsEveryDueOfferingWithTheConfiguredLeadTime() {
        properties.setStartReminderLead(Duration.ofHours(72));
        when(offeringRepository.findIdsDueForStartReminder(any(), any()))
                .thenReturn(List.of(1L, 2L));

        service.sendRemindersForTenant(TENANT_ID);

        verify(processor).remindCourseStart(TENANT_ID, 1L);
        verify(processor).remindCourseStart(TENANT_ID, 2L);

        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> until = ArgumentCaptor.forClass(Instant.class);
        verify(offeringRepository).findIdsDueForStartReminder(now.capture(), until.capture());
        org.assertj.core.api.Assertions.assertThat(
                        Duration.between(now.getValue(), until.getValue()))
                .isEqualTo(Duration.ofHours(72));
    }

    @Test
    void aFailingOfferingDoesNotKeepTheOthersFromBeingReminded() {
        when(offeringRepository.findIdsDueForStartReminder(any(), any()))
                .thenReturn(List.of(1L, 2L));
        when(processor.remindCourseStart(TENANT_ID, 1L))
                .thenThrow(new IllegalStateException("boom"));

        service.sendRemindersForTenant(TENANT_ID);

        verify(processor).remindCourseStart(TENANT_ID, 2L);
    }

    @Test
    void nothingDueSendsNothing() {
        when(offeringRepository.findIdsDueForStartReminder(any(), any())).thenReturn(List.of());

        service.sendRemindersForTenant(TENANT_ID);

        verify(processor, never()).remindCourseStart(any(), any());
    }
}
