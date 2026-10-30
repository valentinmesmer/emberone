/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.domain.CertEvidenceType;
import com.emberone.certification.domain.CertGrantRecipientMode;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.dto.*;
import com.emberone.certification.event.CertificationEventGrant;
import com.emberone.certification.event.CertificationEventGrantRecipient;
import com.emberone.certification.event.CertificationEventGrantRecipientRepository;
import com.emberone.certification.event.CertificationEventGrantRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.event.domain.EventAttendance;
import com.emberone.event.domain.EventType;
import com.emberone.event.repository.EventAttendanceRepository;
import com.emberone.event.service.EventService;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.user.domain.User;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationEventGrantService {

    private final CertificationEventGrantRepository grantRepository;
    private final CertificationEventGrantRecipientRepository recipientRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final UserCertificationRecordRepository recordRepository;
    private final CertificationCatalogService catalogService;
    private final CertificationAssignmentService assignmentService;
    private final CertificationRecordService certificationRecordService;
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService;
    private final EventService eventService;
    private final TokenService tokenService;

    private static void assertGrantMatchesEvent(
            CertificationEventGrant grant, EventType eventType, Long eventId) {
        if (!grant.getEventType().equals(eventType) || !grant.getEventId().equals(eventId)) {
            throw new ResourceNotFoundException(
                    "Certification event grant not found for this event");
        }
    }

    private static String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    @Transactional(readOnly = true)
    public List<CertificationEventGrantDto> listForEvent(
            Long organizationId, EventType eventType, Long eventId) {
        Long tenantId = tokenService.requireTenantId();
        eventService.assertEventInOrganization(eventType, eventId, organizationId);
        return grantRepository.findByEventTypeAndEventId(eventType, eventId).stream()
                .map(grant -> toSummaryDto(grant, tenantId))
                .sorted(
                        Comparator.comparing(
                                CertificationEventGrantDto::getCertDefinitionName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificationEventGrantDetailDto getGrant(
            Long organizationId, EventType eventType, Long eventId, Long grantId) {
        Long tenantId = tokenService.requireTenantId();
        eventService.assertEventInOrganization(eventType, eventId, organizationId);
        CertificationEventGrant grant = requireGrant(grantId);
        assertGrantMatchesEvent(grant, eventType, eventId);
        return toDetailDto(grant, tenantId);
    }

    @Transactional
    public CertificationEventGrantDetailDto createGrant(
            Long organizationId,
            EventType eventType,
            Long eventId,
            CreateCertificationEventGrantRequest request) {
        Long tenantId = tokenService.requireTenantId();
        Long callerUserId = tokenService.getReferencedUserId();
        eventService.assertEventInOrganization(eventType, eventId, organizationId);

        TenantCertDefinition definition =
                requireGrantableDefinition(
                        organizationId, eventType, request.getTenantCertDefinitionId());

        if (grantRepository
                .findByEventTypeAndEventIdAndTenantCertDefinitionId(
                        eventType, eventId, definition.getId())
                .isPresent()) {
            throw new ConflictException(
                    "Certification event grant already exists for this definition");
        }

        validateRecipientRequest(
                eventType, eventId, request.getRecipientMode(), request.getRecipientUserIds());

        LocalDate completedDate =
                eventService.requireEventStartDate(eventType, eventId, organizationId);

        CertificationEventGrant grant =
                grantRepository.save(
                        CertificationEventGrant.builder()
                                .tenantId(tenantId)
                                .organizationId(organizationId)
                                .eventType(eventType)
                                .eventId(eventId)
                                .tenantCertDefinitionId(definition.getId())
                                .recipientMode(request.getRecipientMode())
                                .completedDate(completedDate)
                                .notes(normalizeNotes(request.getNotes()))
                                .createdByUserId(callerUserId)
                                .build());

        replaceSpecificRecipients(
                tenantId, grant, request.getRecipientMode(), request.getRecipientUserIds());

        reconcile(grant);
        log.info(
                "Created certification event grant {} for {} event {}",
                grant.getId(),
                eventType,
                eventId);
        return toDetailDto(grant, tenantId);
    }

    @Transactional
    public CertificationEventGrantDetailDto updateGrant(
            Long organizationId,
            EventType eventType,
            Long eventId,
            Long grantId,
            UpdateCertificationEventGrantRequest request) {
        Long tenantId = tokenService.requireTenantId();
        eventService.assertEventInOrganization(eventType, eventId, organizationId);
        CertificationEventGrant grant = requireGrant(grantId);
        assertGrantMatchesEvent(grant, eventType, eventId);

        validateRecipientRequest(
                eventType, eventId, request.getRecipientMode(), request.getRecipientUserIds());

        grant.setRecipientMode(request.getRecipientMode());
        grant.setCompletedDate(
                eventService.requireEventStartDate(eventType, eventId, organizationId));
        grant.setNotes(normalizeNotes(request.getNotes()));
        grant = grantRepository.save(grant);

        replaceSpecificRecipients(
                tenantId, grant, request.getRecipientMode(), request.getRecipientUserIds());

        reconcile(grant);
        log.info(
                "Updated certification event grant {} for {} event {}",
                grant.getId(),
                eventType,
                eventId);
        return toDetailDto(grant, tenantId);
    }

    /**
     * Removes one event grant, including linked certification records (Nachweise) and EVENT-sourced
     * assignments.
     */
    @Transactional
    public void deleteGrant(Long organizationId, EventType eventType, Long eventId, Long grantId) {
        eventService.assertEventInOrganization(eventType, eventId, organizationId);
        CertificationEventGrant grant = requireGrant(grantId);
        assertGrantMatchesEvent(grant, eventType, eventId);
        deleteGrantCompletely(grant);
        log.info(
                "Removed certification event grant {} for {} event {}",
                grantId,
                eventType,
                eventId);
    }

    /**
     * After a user is added to attendance: reconcile grants so {@code ALL_ATTENDEES} recipients
     * pick up the new person (SPECIFIC grants are unchanged).
     */
    @Transactional
    public void onAttendanceAdded(EventType eventType, Long eventId, Long userId) {
        for (CertificationEventGrant grant :
                grantRepository.findByEventTypeAndEventId(eventType, eventId)) {
            if (grant.getRecipientMode() != CertGrantRecipientMode.ALL_ATTENDEES) {
                continue;
            }
            reconcile(grant);
            log.debug(
                    "Reconciled ALL_ATTENDEES grant {} after attendance add of user {} on event {} {}",
                    grant.getId(),
                    userId,
                    eventType,
                    eventId);
        }
    }

    /**
     * After a user is removed from attendance: drop SPECIFIC recipient rows if needed, then
     * reconcile so linked Nachweise / EVENT assignments are cleaned up.
     */
    @Transactional
    public void onAttendanceRemoved(EventType eventType, Long eventId, Long userId) {
        for (CertificationEventGrant grant :
                grantRepository.findByEventTypeAndEventId(eventType, eventId)) {
            if (grant.getRecipientMode() == CertGrantRecipientMode.SPECIFIC) {
                recipientRepository.deleteByGrantIdAndUserId(grant.getId(), userId);
            }
            reconcile(grant);
            log.debug(
                    "Reconciled grant {} after attendance remove of user {} on event {} {}",
                    grant.getId(),
                    userId,
                    eventType,
                    eventId);
        }
    }

    /**
     * Removes all event grants for a deleted service/operation event, including linked
     * certification records (Nachweise) and EVENT-sourced assignments.
     */
    @Transactional
    public void deleteGrantsForEvent(EventType eventType, Long eventId) {
        List<CertificationEventGrant> grants =
                grantRepository.findByEventTypeAndEventId(eventType, eventId);
        for (CertificationEventGrant grant : grants) {
            deleteGrantCompletely(grant);
            log.info(
                    "Removed certification event grant {} for deleted event {} {}",
                    grant.getId(),
                    eventType,
                    eventId);
        }
    }

    private void deleteGrantCompletely(CertificationEventGrant grant) {
        Long grantId = grant.getId();
        for (UserCertificationRecord record :
                recordRepository.findAllByAppLinkTypeAndAppLinkId(
                        AppLinkType.CERTIFICATION_EVENT_GRANT, grantId)) {
            certificationRecordService.deleteFromEventGrant(record);
        }
        assignmentService.deleteAssignmentsForEventGrant(grantId);
        grantRepository.delete(grant);
    }

    void reconcile(CertificationEventGrant grant) {
        Long tenantId = grant.getTenantId();
        Set<Long> recipients = resolveEffectiveRecipients(grant);
        Map<Long, UserCertificationRecord> existingByUser =
                recordRepository
                        .findAllByAppLinkTypeAndAppLinkId(
                                AppLinkType.CERTIFICATION_EVENT_GRANT, grant.getId())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        UserCertificationRecord::getUserId, r -> r, (a, b) -> a));

        for (Long userId : recipients) {
            UserCertificationRecord existing = existingByUser.remove(userId);
            if (existing != null) {
                certificationRecordService.syncFromEventGrant(existing, grant);
                continue;
            }
            assignmentService.ensureAssignmentForEvent(
                    tenantId,
                    userId,
                    grant.getTenantCertDefinitionId(),
                    grant.getOrganizationId(),
                    grant.getId());
            certificationRecordService.createFromEventGrant(
                    tenantId, userId, grant.getTenantCertDefinitionId(), grant);
        }

        for (UserCertificationRecord orphan : existingByUser.values()) {
            certificationRecordService.deleteFromEventGrant(orphan);
        }
        assignmentService.deleteOrphanAssignmentsForEventGrant(grant.getId(), recipients);
    }

    private TenantCertDefinition requireGrantableDefinition(
            Long organizationId, EventType eventType, Long definitionId) {
        TenantCertDefinition definition =
                tenantDefinitionRepository
                        .findById(definitionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Certification definition not found: "
                                                        + definitionId));
        if (!LifecycleFields.isCatalogSelectable(definition)) {
            throw new ConflictException("Certification definition is not available");
        }
        catalogService.requireTypeAvailableForOrg(definition.getId(), organizationId);

        Set<CertEvidenceType> evidenceTypes =
                evidenceTypeService.getEvidenceTypes(definition.getId());
        if (!TenantCertDefinitionEvidenceTypeService.allowsEventGrantForEventType(
                evidenceTypes, eventType)) {
            throw new ConflictException(
                    "Event-based certification grant is not available for this definition");
        }
        return definition;
    }

    private void validateRecipientRequest(
            EventType eventType,
            Long eventId,
            CertGrantRecipientMode recipientMode,
            List<Long> recipientUserIds) {
        if (recipientMode == CertGrantRecipientMode.ALL_ATTENDEES) {
            return;
        }
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw new ConflictException("recipientUserIds is required for SPECIFIC recipient mode");
        }
        Set<Long> attendeeIds =
                eventAttendanceRepository
                        .findByEventTypeAndEventIdWithUser(eventType, eventId)
                        .stream()
                        .map(attendance -> attendance.getUser().getId())
                        .collect(Collectors.toSet());
        for (Long userId : recipientUserIds) {
            if (!attendeeIds.contains(userId)) {
                throw new ConflictException("User is not an attendee of this event: " + userId);
            }
        }
    }

    private void replaceSpecificRecipients(
            Long tenantId,
            CertificationEventGrant grant,
            CertGrantRecipientMode recipientMode,
            List<Long> recipientUserIds) {
        if (recipientMode == CertGrantRecipientMode.ALL_ATTENDEES) {
            for (CertificationEventGrantRecipient existing :
                    recipientRepository.findByGrantId(grant.getId())) {
                recipientRepository.delete(existing);
            }
            return;
        }
        Set<Long> requested = new LinkedHashSet<>(recipientUserIds);
        List<CertificationEventGrantRecipient> existing =
                recipientRepository.findByGrantId(grant.getId());
        for (CertificationEventGrantRecipient row : existing) {
            if (!requested.contains(row.getUserId())) {
                recipientRepository.delete(row);
            }
        }
        Set<Long> existingIds =
                existing.stream()
                        .map(CertificationEventGrantRecipient::getUserId)
                        .collect(Collectors.toSet());
        for (Long userId : requested) {
            if (existingIds.contains(userId)) {
                continue;
            }
            recipientRepository.save(
                    CertificationEventGrantRecipient.builder()
                            .tenantId(tenantId)
                            .grantId(grant.getId())
                            .userId(userId)
                            .build());
        }
    }

    private Set<Long> resolveEffectiveRecipients(CertificationEventGrant grant) {
        if (grant.getRecipientMode() == CertGrantRecipientMode.SPECIFIC) {
            return recipientRepository.findByGrantId(grant.getId()).stream()
                    .map(CertificationEventGrantRecipient::getUserId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return eventAttendanceRepository
                .findByEventTypeAndEventIdWithUser(grant.getEventType(), grant.getEventId())
                .stream()
                .map(attendance -> attendance.getUser().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean recordDiffersFromGrant(
            UserCertificationRecord record, CertificationEventGrant grant) {
        if (!record.getCompletedDate().equals(grant.getCompletedDate())) {
            return true;
        }
        String recordNotes = normalizeNotes(record.getNotes());
        String grantNotes = normalizeNotes(grant.getNotes());
        return !Objects.equals(recordNotes, grantNotes);
    }

    private CertificationEventGrantDetailDto toDetailDto(
            CertificationEventGrant grant, Long tenantId) {
        Set<Long> inScopeRecipients = resolveEffectiveRecipients(grant);
        Map<Long, UserCertificationRecord> recordsByUser =
                recordRepository
                        .findAllByAppLinkTypeAndAppLinkId(
                                AppLinkType.CERTIFICATION_EVENT_GRANT, grant.getId())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        UserCertificationRecord::getUserId, r -> r, (a, b) -> a));

        Set<Long> allUserIds = new LinkedHashSet<>(inScopeRecipients);
        allUserIds.addAll(recordsByUser.keySet());

        Map<Long, EventAttendance> attendanceByUser =
                eventAttendanceRepository
                        .findByEventTypeAndEventIdWithUser(grant.getEventType(), grant.getEventId())
                        .stream()
                        .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a, (a, b) -> a));

        List<CertificationEventGrantRecipientDto> recipients = new ArrayList<>();
        for (Long userId : allUserIds) {
            EventAttendance attendance = attendanceByUser.get(userId);
            User user = attendance != null ? attendance.getUser() : null;
            UserCertificationRecord record = recordsByUser.get(userId);
            boolean inScope = inScopeRecipients.contains(userId);
            boolean edited = record != null && recordDiffersFromGrant(record, grant);
            recipients.add(
                    CertificationEventGrantRecipientDto.builder()
                            .userId(userId)
                            .firstName(user != null ? user.getFirstName() : null)
                            .lastName(user != null ? user.getLastName() : null)
                            .inScope(inScope)
                            .recordId(record != null ? record.getId() : null)
                            .edited(edited)
                            .build());
        }
        recipients.sort(
                Comparator.comparing(
                                CertificationEventGrantRecipientDto::getLastName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(
                                CertificationEventGrantRecipientDto::getFirstName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return CertificationEventGrantDetailDto.builder()
                .grant(toSummaryDto(grant, tenantId))
                .recipients(recipients)
                .build();
    }

    private CertificationEventGrantDto toSummaryDto(CertificationEventGrant grant, Long tenantId) {
        String definitionName =
                tenantDefinitionRepository
                        .findById(grant.getTenantCertDefinitionId())
                        .map(TenantCertDefinition::getName)
                        .orElse(null);
        int recipientCount =
                grant.getRecipientMode() == CertGrantRecipientMode.ALL_ATTENDEES
                        ? eventAttendanceRepository
                                .findByEventTypeAndEventIdWithUser(
                                        grant.getEventType(), grant.getEventId())
                                .size()
                        : recipientRepository.findByGrantId(grant.getId()).size();
        int recordCount =
                recordRepository
                        .findAllByAppLinkTypeAndAppLinkId(
                                AppLinkType.CERTIFICATION_EVENT_GRANT, grant.getId())
                        .size();
        return CertificationEventGrantDto.builder()
                .id(grant.getId())
                .organizationId(grant.getOrganizationId())
                .eventType(grant.getEventType())
                .eventId(grant.getEventId())
                .tenantCertDefinitionId(grant.getTenantCertDefinitionId())
                .certDefinitionName(definitionName)
                .recipientMode(grant.getRecipientMode())
                .completedDate(grant.getCompletedDate())
                .notes(grant.getNotes())
                .createdByUserId(grant.getCreatedByUserId())
                .recipientCount(recipientCount)
                .recordCount(recordCount)
                .build();
    }

    private CertificationEventGrant requireGrant(Long grantId) {
        return grantRepository
                .findById(grantId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Certification event grant not found: " + grantId));
    }
}
