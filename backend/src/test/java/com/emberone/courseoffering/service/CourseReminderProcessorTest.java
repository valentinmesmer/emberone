/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class CourseReminderProcessorTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long OFFERING_ID = 100L;
    private static final long TYPE_ID = 5L;

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock NotificationService notificationService;
    @Mock PlatformTransactionManager transactionManager;
    @Mock TransactionStatus transactionStatus;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    @InjectMocks CourseReminderProcessor processor;

    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        offering = offering();
        lenient().when(offeringRepository.findById(OFFERING_ID)).thenReturn(Optional.of(offering));
        lenient()
                .when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenReturn(Optional.of(offering));
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(TYPE_ID);
        type.setTenantId(TENANT_ID);
        type.setName("Atemschutz");
        lenient()
                .when(trainingCourseTypeRepository.findById(TYPE_ID))
                .thenReturn(java.util.Optional.of(type));
    }

    @Test
    void startReminderGoesToParticipantsAndWaitlist() {
        when(registrationRepository.findUserIdsByOfferingAndStatusIn(
                        OFFERING_ID,
                        List.of(
                                CourseRegistrationStatus.PARTICIPANT,
                                CourseRegistrationStatus.WAITLIST)))
                .thenReturn(List.of(3L, 4L));

        boolean handled = processor.remindCourseStart(TENANT_ID, OFFERING_ID);

        assertThat(handled).isTrue();
        NotificationRequest request = capturedNotification();
        assertThat(request.type()).isEqualTo(NotificationType.COURSE_OFFERING_START_REMINDER);
        assertThat(request.recipientUserIds()).containsExactlyInAnyOrder(3L, 4L);
        assertThat(request.params())
                .containsEntry(NotificationParamKeys.COURSE_OFFERING_TOPIC, "Atemschutz")
                .containsKey(NotificationParamKeys.COURSE_OFFERING_START);
        assertThat(offering.getStartReminderSentAt()).isNotNull();
    }

    private NotificationRequest capturedNotification() {
        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        return captor.getValue();
    }

    private static CourseOffering offering() {
        Instant now = Instant.now();
        return CourseOffering.builder()
                .id(OFFERING_ID)
                .tenantId(TENANT_ID)
                .organizationId(ORG_ID)
                .trainingCourseTypeId(TYPE_ID)
                .status(CourseOfferingStatus.PUBLISHED)
                .registrationStart(now.minus(10, ChronoUnit.DAYS))
                .registrationEnd(now.plus(1, ChronoUnit.DAYS))
                .firstSessionStart(now.plus(2, ChronoUnit.DAYS))
                .lastSessionEnd(now.plus(3, ChronoUnit.DAYS))
                .maxParticipants(10)
                .sessions(new ArrayList<>())
                .audiences(new ArrayList<>())
                .prerequisites(new ArrayList<>())
                .build();
    }
}
