/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.certification.domain.CertGrantRecipientMode;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.event.CertificationEventGrant;
import com.emberone.certification.event.CertificationEventGrantRecipientRepository;
import com.emberone.certification.event.CertificationEventGrantRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.event.domain.EventType;
import com.emberone.event.repository.EventAttendanceRepository;
import com.emberone.event.service.EventService;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificationEventGrantServiceTest {

    @Mock CertificationEventGrantRepository grantRepository;
    @Mock CertificationEventGrantRecipientRepository recipientRepository;
    @Mock EventAttendanceRepository eventAttendanceRepository;
    @Mock TenantCertDefinitionRepository tenantDefinitionRepository;
    @Mock UserCertificationRecordRepository recordRepository;
    @Mock CertificationCatalogService catalogService;
    @Mock CertificationAssignmentService assignmentService;
    @Mock CertificationRecordService certificationRecordService;
    @Mock TenantCertDefinitionEvidenceTypeService evidenceTypeService;
    @Mock EventService eventService;
    @Mock TokenService tokenService;
    @InjectMocks CertificationEventGrantService service;

    @Test
    void deleteGrantsForEventRemovesRecordsAssignmentsAndGrants() {
        CertificationEventGrant grant =
                CertificationEventGrant.builder()
                        .id(9L)
                        .tenantId(1L)
                        .eventType(EventType.SERVICE)
                        .eventId(5L)
                        .build();
        UserCertificationRecord record =
                UserCertificationRecord.builder().id(44L).tenantId(1L).userId(7L).build();

        when(grantRepository.findByEventTypeAndEventId(EventType.SERVICE, 5L))
                .thenReturn(List.of(grant));
        when(recordRepository.findAllByAppLinkTypeAndAppLinkId(
                        AppLinkType.CERTIFICATION_EVENT_GRANT, 9L))
                .thenReturn(List.of(record));

        service.deleteGrantsForEvent(EventType.SERVICE, 5L);

        verify(certificationRecordService).deleteFromEventGrant(record);
        verify(assignmentService).deleteAssignmentsForEventGrant(9L);
        verify(grantRepository).delete(grant);
    }

    @Test
    void deleteGrantRemovesOnlyTheRequestedGrant() {
        CertificationEventGrant grant =
                CertificationEventGrant.builder()
                        .id(9L)
                        .tenantId(1L)
                        .organizationId(2L)
                        .eventType(EventType.SERVICE)
                        .eventId(5L)
                        .tenantCertDefinitionId(3L)
                        .build();
        UserCertificationRecord record =
                UserCertificationRecord.builder().id(44L).tenantId(1L).userId(7L).build();

        when(grantRepository.findById(9L)).thenReturn(Optional.of(grant));
        when(recordRepository.findAllByAppLinkTypeAndAppLinkId(
                        AppLinkType.CERTIFICATION_EVENT_GRANT, 9L))
                .thenReturn(List.of(record));

        service.deleteGrant(2L, EventType.SERVICE, 5L, 9L);

        verify(eventService).assertEventInOrganization(EventType.SERVICE, 5L, 2L);
        verify(certificationRecordService).deleteFromEventGrant(record);
        verify(assignmentService).deleteAssignmentsForEventGrant(9L);
        verify(grantRepository).delete(grant);
    }

    @Test
    void reconcileSyncsExistingRemovesOrphansAndCreatesMissing() {
        CertificationEventGrant grant =
                CertificationEventGrant.builder()
                        .id(9L)
                        .tenantId(1L)
                        .organizationId(2L)
                        .eventType(EventType.SERVICE)
                        .eventId(5L)
                        .tenantCertDefinitionId(3L)
                        .recipientMode(CertGrantRecipientMode.SPECIFIC)
                        .completedDate(LocalDate.of(2026, 8, 12))
                        .build();
        UserCertificationRecord keep =
                UserCertificationRecord.builder().id(44L).tenantId(1L).userId(7L).build();
        UserCertificationRecord orphan =
                UserCertificationRecord.builder().id(45L).tenantId(1L).userId(8L).build();

        when(recipientRepository.findByGrantId(9L))
                .thenReturn(
                        List.of(
                                com.emberone.certification.event.CertificationEventGrantRecipient
                                        .builder()
                                        .tenantId(1L)
                                        .grantId(9L)
                                        .userId(7L)
                                        .build(),
                                com.emberone.certification.event.CertificationEventGrantRecipient
                                        .builder()
                                        .tenantId(1L)
                                        .grantId(9L)
                                        .userId(9L)
                                        .build()));
        when(recordRepository.findAllByAppLinkTypeAndAppLinkId(
                        AppLinkType.CERTIFICATION_EVENT_GRANT, 9L))
                .thenReturn(List.of(keep, orphan));

        service.reconcile(grant);

        verify(certificationRecordService).syncFromEventGrant(keep, grant);
        verify(certificationRecordService).deleteFromEventGrant(orphan);
        verify(assignmentService).ensureAssignmentForEvent(1L, 9L, 3L, 2L, 9L);
        verify(certificationRecordService).createFromEventGrant(1L, 9L, 3L, grant);
        verify(assignmentService).deleteOrphanAssignmentsForEventGrant(9L, Set.of(7L, 9L));
        verify(certificationRecordService, never())
                .createFromEventGrant(eq(1L), eq(7L), any(), any());
    }

    @Test
    void onAttendanceAddedReconcilesOnlyAllAttendeesGrants() {
        CertificationEventGrant allAttendees =
                CertificationEventGrant.builder()
                        .id(9L)
                        .tenantId(1L)
                        .organizationId(2L)
                        .eventType(EventType.SERVICE)
                        .eventId(5L)
                        .tenantCertDefinitionId(3L)
                        .recipientMode(CertGrantRecipientMode.ALL_ATTENDEES)
                        .completedDate(LocalDate.of(2026, 8, 12))
                        .build();
        CertificationEventGrant specific =
                CertificationEventGrant.builder()
                        .id(10L)
                        .tenantId(1L)
                        .organizationId(2L)
                        .eventType(EventType.SERVICE)
                        .eventId(5L)
                        .tenantCertDefinitionId(4L)
                        .recipientMode(CertGrantRecipientMode.SPECIFIC)
                        .completedDate(LocalDate.of(2026, 8, 12))
                        .build();

        when(grantRepository.findByEventTypeAndEventId(EventType.SERVICE, 5L))
                .thenReturn(List.of(allAttendees, specific));
        when(eventAttendanceRepository.findByEventTypeAndEventIdWithUser(EventType.SERVICE, 5L))
                .thenReturn(List.of());
        when(recordRepository.findAllByAppLinkTypeAndAppLinkId(
                        AppLinkType.CERTIFICATION_EVENT_GRANT, 9L))
                .thenReturn(List.of());

        service.onAttendanceAdded(EventType.SERVICE, 5L, 7L);

        verify(recordRepository)
                .findAllByAppLinkTypeAndAppLinkId(AppLinkType.CERTIFICATION_EVENT_GRANT, 9L);
        verify(recordRepository, never())
                .findAllByAppLinkTypeAndAppLinkId(AppLinkType.CERTIFICATION_EVENT_GRANT, 10L);
        verify(recipientRepository, never()).deleteByGrantIdAndUserId(any(), any());
    }

    @Test
    void onAttendanceRemovedDropsSpecificRecipientAndReconciles() {
        CertificationEventGrant specific =
                CertificationEventGrant.builder()
                        .id(9L)
                        .tenantId(1L)
                        .organizationId(2L)
                        .eventType(EventType.SERVICE)
                        .eventId(5L)
                        .tenantCertDefinitionId(3L)
                        .recipientMode(CertGrantRecipientMode.SPECIFIC)
                        .completedDate(LocalDate.of(2026, 8, 12))
                        .build();
        UserCertificationRecord record =
                UserCertificationRecord.builder().id(44L).tenantId(1L).userId(7L).build();

        when(grantRepository.findByEventTypeAndEventId(EventType.SERVICE, 5L))
                .thenReturn(List.of(specific));
        when(recipientRepository.findByGrantId(9L)).thenReturn(List.of());
        when(recordRepository.findAllByAppLinkTypeAndAppLinkId(
                        AppLinkType.CERTIFICATION_EVENT_GRANT, 9L))
                .thenReturn(List.of(record));

        service.onAttendanceRemoved(EventType.SERVICE, 5L, 7L);

        verify(recipientRepository).deleteByGrantIdAndUserId(9L, 7L);
        verify(certificationRecordService).deleteFromEventGrant(record);
        verify(assignmentService).deleteOrphanAssignmentsForEventGrant(9L, Set.of());
    }
}
