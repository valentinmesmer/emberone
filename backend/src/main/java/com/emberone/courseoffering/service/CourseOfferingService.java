/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingPrerequisite;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.domain.CourseSessionType;
import com.emberone.courseoffering.dto.CourseAudienceEntryDto;
import com.emberone.courseoffering.dto.CourseAudienceEntryRequest;
import com.emberone.courseoffering.dto.CourseAudienceQuotaRequest;
import com.emberone.courseoffering.dto.CourseOfferingDto;
import com.emberone.courseoffering.dto.CourseOfferingPageDto;
import com.emberone.courseoffering.dto.CoursePrerequisiteEntryRequest;
import com.emberone.courseoffering.dto.CoursePrerequisitesDto;
import com.emberone.courseoffering.dto.CourseSessionDto;
import com.emberone.courseoffering.dto.CourseSessionRequest;
import com.emberone.courseoffering.dto.CreateCourseOfferingRequest;
import com.emberone.courseoffering.dto.ReplaceCoursePrerequisitesRequest;
import com.emberone.courseoffering.dto.UpdateCourseOfferingRequest;
import com.emberone.courseoffering.event.CourseOfferingSessionsChangedEvent;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.list.CourseOfferingListDefinition;
import com.emberone.courseoffering.list.CourseOfferingListFilter;
import com.emberone.courseoffering.list.CourseOfferingListSpecs;
import com.emberone.courseoffering.list.CourseOfferingTemplateListDefinition;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.repository.OrganizationTypeDefinitionRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListDefinition;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages course offerings and their sessions for the offering organization. */
@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseOfferingCatalogService catalogService;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final TenantCertDefinitionRepository certDefinitionRepository;
    private final TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationTypeDefinitionRepository organizationTypeRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public CourseOfferingPageDto list(
            Long organizationId, ListQuery query, CourseOfferingListFilter filter) {
        Long tenantId = tokenService.requireTenantId();
        return page(
                tenantId,
                query,
                CourseOfferingListDefinition.INSTANCE,
                Specification.allOf(
                        CourseOfferingListSpecs.forOrganization(organizationId),
                        CourseOfferingListSpecs.withStatus(filter.statuses()),
                        CourseOfferingListSpecs.withTrainingCourseTypeIds(
                                filter.trainingCourseTypeIds())));
    }

    @Transactional(readOnly = true)
    public List<ListFilterOptionDto> listTrainingCourseTypeFilterOptions(Long organizationId) {
        Long tenantId = tokenService.requireTenantId();
        List<Long> typeIds =
                offeringRepository.findDistinctTrainingCourseTypeIdsByOrganization(organizationId);
        if (typeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names =
                trainingCourseTypeRepository.findAllById(typeIds).stream()
                        .filter(type -> tenantId.equals(type.getTenantId()))
                        .collect(
                                Collectors.toMap(
                                        TenantTrainingCourseType::getId,
                                        TenantTrainingCourseType::getName,
                                        (a, b) -> a));
        return typeIds.stream()
                .map(
                        typeId ->
                                new ListFilterOptionDto(
                                        String.valueOf(typeId),
                                        names.getOrDefault(typeId, String.valueOf(typeId))))
                .sorted(
                        Comparator.comparing(
                                ListFilterOptionDto::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseOfferingPageDto listTemplates(Long organizationId, ListQuery query) {
        Long tenantId = tokenService.requireTenantId();
        return page(
                tenantId,
                query,
                CourseOfferingTemplateListDefinition.INSTANCE,
                Specification.allOf(
                        CourseOfferingListSpecs.forOrganization(organizationId),
                        CourseOfferingListSpecs.templates()));
    }

    private CourseOfferingPageDto page(
            Long tenantId,
            ListQuery query,
            ListDefinition<CourseOffering> definition,
            Specification<CourseOffering> scope) {
        Page<CourseOffering> page =
                offeringRepository.findAll(
                        Specification.allOf(scope, definition.searchSpecification(query)),
                        definition.pageable(query));
        List<Long> ids = page.getContent().stream().map(CourseOffering::getId).toList();
        List<CourseOffering> hydrated =
                ids.isEmpty() ? List.of() : offeringRepository.findByIdIn(ids);
        List<CourseOffering> ordered =
                PageReorder.reorderByIds(ids, hydrated, CourseOffering::getId);
        Map<Long, String> typeNames = loadTypeNames(tenantId, ordered);
        return CourseOfferingPageDto.builder()
                .content(ordered.stream().map(o -> toDto(o, typeNames)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public CourseOfferingDto get(Long organizationId, Long offeringId) {
        CourseOffering offering = requireOffering(organizationId, offeringId);
        return toDto(offering, loadTypeNames(offering.getTenantId(), List.of(offering)));
    }

    @Transactional
    public CourseOfferingDto create(Long organizationId, CreateCourseOfferingRequest request) {
        catalogService.requireOfferable(request.getTrainingCourseTypeId(), organizationId);
        if (request.getAudienceQuotas() != null && !request.getAudienceQuotas().isEmpty()) {
            throw new CourseOfferingValidationException(
                    "Quotas are set on the audience entries, which a new offering does not have yet");
        }
        CourseOffering offering =
                CourseOffering.builder()
                        .tenantId(tokenService.requireTenantId())
                        .organizationId(organizationId)
                        .trainingCourseTypeId(request.getTrainingCourseTypeId())
                        .status(CourseOfferingStatus.DRAFT)
                        .build();
        applyFields(offering, request);
        return save(offering);
    }

    @Transactional
    public CourseOfferingDto update(
            Long organizationId, Long offeringId, UpdateCourseOfferingRequest request) {
        CourseOffering offering = requireEditableForUpdate(organizationId, offeringId);
        applyFields(offering, request);
        if (request.getAudienceQuotas() != null) {
            applyQuotas(offering, request.getAudienceQuotas());
        }
        requireValidQuotas(offering);
        requireQuotasFitRegistrations(offering, Set.of());
        return saveAndRevalidate(offering);
    }

    @Transactional
    public void delete(Long organizationId, Long offeringId) {
        CourseOffering offering = requireOffering(organizationId, offeringId);
        if (offering.getStatus() != CourseOfferingStatus.DRAFT
                && offering.getStatus() != CourseOfferingStatus.TEMPLATE) {
            throw new CourseOfferingValidationException("Only drafts and templates can be deleted");
        }
        offeringRepository.delete(offering);
    }

    @Transactional
    public CourseOfferingDto publish(Long organizationId, Long offeringId) {
        CourseOffering offering = requireOffering(organizationId, offeringId);
        if (offering.getStatus() != CourseOfferingStatus.DRAFT) {
            throw new CourseOfferingValidationException("Only draft offerings can be published");
        }
        catalogService.requireOfferable(offering.getTrainingCourseTypeId(), organizationId);
        validatePublishable(offering);
        offering.setStatus(CourseOfferingStatus.PUBLISHED);
        return save(offering);
    }

    /**
     * Replaces the audience. Entries are matched by kind and reference: a kept entry keeps its id
     * (and with it the participants assigned to it) and only takes the new quota. Taking the
     * offering lock first keeps this from racing with an approval that reads the quotas.
     */
    @Transactional
    public CourseOfferingDto replaceAudiences(
            Long organizationId, Long offeringId, List<CourseAudienceEntryRequest> entries) {
        CourseOffering offering = requireEditableForUpdate(organizationId, offeringId);
        validateAudience(offering.getTenantId(), entries);
        Map<String, CourseOfferingAudience> existing = new HashMap<>();
        offering.getAudiences().forEach(entry -> existing.put(audienceKey(entry), entry));
        List<CourseOfferingAudience> next = new ArrayList<>();
        for (CourseAudienceEntryRequest request : entries) {
            CourseOfferingAudience entry =
                    existing.remove(request.getKind() + ":" + request.getRefId());
            if (entry == null) {
                entry =
                        CourseOfferingAudience.builder()
                                .tenantId(offering.getTenantId())
                                .offering(offering)
                                .kind(request.getKind())
                                .refId(request.getRefId())
                                .build();
            }
            entry.setQuota(request.getQuota());
            next.add(entry);
        }
        Set<Long> removedIds =
                existing.values().stream()
                        .map(CourseOfferingAudience::getId)
                        .collect(Collectors.toSet());
        offering.getAudiences().removeAll(existing.values());
        next.stream()
                .filter(entry -> !offering.getAudiences().contains(entry))
                .forEach(entry -> offering.getAudiences().add(entry));
        requireValidQuotas(offering);
        requireQuotasFitRegistrations(offering, removedIds);
        return saveAndRevalidate(offering);
    }

    /**
     * Replaces prerequisite modes and entries. Existing rows matched by kind and config key are
     * kept (same pattern as {@link #replaceAudiences}); only missing keys are removed and new ones
     * added, so the unique constraint is never violated by delete-then-reinsert.
     */
    @Transactional
    public CourseOfferingDto replacePrerequisites(
            Long organizationId, Long offeringId, ReplaceCoursePrerequisitesRequest request) {
        CourseOffering offering = requireEditable(organizationId, offeringId);
        validatePrerequisites(request.getEntries());
        offering.setTrainingCourseRequirementMode(request.getTrainingCourseRequirementMode());
        offering.setCertRequirementMode(request.getCertRequirementMode());
        offering.setDrivingLicenseRequirementMode(request.getDrivingLicenseRequirementMode());

        Map<String, CourseOfferingPrerequisite> existing = new HashMap<>();
        offering.getPrerequisites().forEach(entry -> existing.put(prerequisiteKey(entry), entry));
        List<CourseOfferingPrerequisite> next = new ArrayList<>();
        for (CoursePrerequisiteEntryRequest entry : request.getEntries()) {
            String configKey = entry.getConfigKey().trim();
            CourseOfferingPrerequisite row = existing.remove(entry.getKind() + ":" + configKey);
            if (row == null) {
                row =
                        CourseOfferingPrerequisite.builder()
                                .tenantId(offering.getTenantId())
                                .offering(offering)
                                .kind(entry.getKind())
                                .configKey(configKey)
                                .build();
            }
            next.add(row);
        }
        offering.getPrerequisites().removeAll(existing.values());
        next.stream()
                .filter(entry -> !offering.getPrerequisites().contains(entry))
                .forEach(entry -> offering.getPrerequisites().add(entry));
        return saveAndRevalidate(offering);
    }

    @Transactional
    public CourseOfferingDto addSession(
            Long organizationId, Long offeringId, CourseSessionRequest request) {
        CourseOffering offering = requireEditableForUpdate(organizationId, offeringId);
        CourseSession session = CourseSession.builder().tenantId(offering.getTenantId()).build();
        session.setOffering(offering);
        applySession(session, request);
        offering.getSessions().add(session);
        return saveAndNotifySessionChange(offering, true);
    }

    @Transactional
    public CourseOfferingDto updateSession(
            Long organizationId, Long offeringId, Long sessionId, CourseSessionRequest request) {
        CourseOffering offering = requireEditableForUpdate(organizationId, offeringId);
        CourseSession session = requireSession(offering, sessionId);
        boolean scheduleChanged = changesSchedule(session, request);
        applySession(session, request);
        return saveAndNotifySessionChange(offering, scheduleChanged);
    }

    @Transactional
    public CourseOfferingDto deleteSession(Long organizationId, Long offeringId, Long sessionId) {
        CourseOffering offering = requireEditableForUpdate(organizationId, offeringId);
        CourseSession session = requireSession(offering, sessionId);
        offering.getSessions().remove(session);
        return saveAndNotifySessionChange(offering, true);
    }

    /** Date, time or location differ; type and notes alone are no reason to notify. */
    private static boolean changesSchedule(CourseSession session, CourseSessionRequest request) {
        return !session.getStart().equals(request.getStart())
                || !session.getEnd().equals(request.getEnd())
                || !Objects.equals(session.getLocation(), request.getLocation());
    }

    /**
     * Saves a session change and, for a published offering whose schedule changed, tells the
     * participants and waitlisted users once for this change set. The offering lock held by the
     * caller keeps registrations from moving between the lookup and the commit.
     */
    private CourseOfferingDto saveAndNotifySessionChange(
            CourseOffering offering, boolean scheduleChanged) {
        CourseOfferingDto saved = saveAndRevalidate(offering);
        if (scheduleChanged && offering.getStatus() == CourseOfferingStatus.PUBLISHED) {
            Set<Long> userIds =
                    new LinkedHashSet<>(
                            registrationRepository.findUserIdsByOfferingAndStatusIn(
                                    offering.getId(),
                                    List.of(
                                            CourseRegistrationStatus.PARTICIPANT,
                                            CourseRegistrationStatus.WAITLIST)));
            if (!userIds.isEmpty()) {
                eventPublisher.publishEvent(
                        new CourseOfferingSessionsChangedEvent(
                                offering.getTenantId(),
                                offering.getId(),
                                resolveTypeName(offering),
                                userIds));
            }
        }
        return saved;
    }

    CourseOffering requireOffering(Long organizationId, Long offeringId) {
        return offeringRepository
                .findByIdAndOrganizationId(offeringId, organizationId)
                .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
    }

    /**
     * Like {@link #requireEditable} but under the offering's row lock, the same one approvals take
     * before they read the maximum and the quotas, so a change cannot interleave with a placement.
     */
    private CourseOffering requireEditableForUpdate(Long organizationId, Long offeringId) {
        CourseOffering offering =
                offeringRepository
                        .findForUpdate(offeringId)
                        .filter(found -> found.getOrganizationId().equals(organizationId))
                        .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
        if (!offering.getStatus().isEditable()) {
            throw new CourseOfferingValidationException(
                    "Offering in status " + offering.getStatus() + " cannot be changed");
        }
        return offering;
    }

    private CourseOffering requireEditable(Long organizationId, Long offeringId) {
        CourseOffering offering = requireOffering(organizationId, offeringId);
        if (!offering.getStatus().isEditable()) {
            throw new CourseOfferingValidationException(
                    "Offering in status " + offering.getStatus() + " cannot be changed");
        }
        return offering;
    }

    private CourseSession requireSession(CourseOffering offering, Long sessionId) {
        return offering.getSessions().stream()
                .filter(session -> session.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(
                        () ->
                                new CourseOfferingValidationException(
                                        "Session not found: " + sessionId));
    }

    private void applyFields(CourseOffering offering, UpdateCourseOfferingRequest request) {
        Instant start = request.getRegistrationStart();
        Instant end = request.getRegistrationEnd();
        if (start != null && end != null && !start.isBefore(end)) {
            throw new CourseOfferingValidationException(
                    "Registration start must be before registration end");
        }
        if (offering.getStatus() == CourseOfferingStatus.TEMPLATE) {
            String templateName = request.getTemplateName();
            if (templateName == null || templateName.isBlank()) {
                throw new CourseOfferingValidationException("Template name is required");
            }
            offering.setTemplateName(templateName.trim());
        }
        offering.setContent(normalizeOptionalText(request.getContent()));
        offering.setNotes(normalizeOptionalText(request.getNotes()));
        offering.setRegistrationStart(start);
        offering.setRegistrationEnd(end);
        offering.setMaxParticipants(request.getMaxParticipants());
        offering.setHasExam(request.isHasExam());
        offering.setRequiresHomeOrgApproval(request.isRequiresHomeOrgApproval());
    }

    private void applySession(CourseSession session, CourseSessionRequest request) {
        if (!request.getStart().isBefore(request.getEnd())) {
            throw new CourseOfferingValidationException("Session start must be before its end");
        }
        session.setType(request.getType());
        session.setStart(request.getStart());
        session.setEnd(request.getEnd());
        session.setLocation(request.getLocation());
        session.setNotes(request.getNotes());
    }

    /** A published offering must stay publishable after every change. */
    private CourseOfferingDto saveAndRevalidate(CourseOffering offering) {
        if (offering.getStatus() == CourseOfferingStatus.PUBLISHED) {
            validatePublishable(offering);
        }
        return save(offering);
    }

    private static String audienceKey(CourseOfferingAudience entry) {
        return entry.getKind() + ":" + entry.getRefId();
    }

    private static String prerequisiteKey(CourseOfferingPrerequisite entry) {
        return entry.getKind() + ":" + entry.getConfigKey();
    }

    /** Sets the quota of every existing entry; entries not listed lose theirs. */
    private void applyQuotas(CourseOffering offering, List<CourseAudienceQuotaRequest> quotas) {
        Map<Long, Integer> byAudience = new HashMap<>();
        for (CourseAudienceQuotaRequest quota : quotas) {
            if (byAudience.put(quota.getAudienceId(), quota.getQuota()) != null) {
                throw new CourseOfferingValidationException("Duplicate quota entry");
            }
        }
        Set<Long> known =
                offering.getAudiences().stream()
                        .map(CourseOfferingAudience::getId)
                        .collect(Collectors.toSet());
        if (!known.containsAll(byAudience.keySet())) {
            throw new CourseOfferingValidationException("Unknown audience entry in quotas");
        }
        offering.getAudiences().forEach(entry -> entry.setQuota(byAudience.get(entry.getId())));
    }

    /**
     * Quotas are all or nothing and must not add up to more than the maximum. {@code OWN_ORG}
     * stands alone and takes no quota; a quota on the only entry would only repeat the maximum.
     * Checked after every change to the maximum or the audience, also on drafts and templates, and
     * again when publishing.
     */
    private void requireValidQuotas(CourseOffering offering) {
        List<CourseOfferingAudience> entries = offering.getAudiences();
        long withQuota = entries.stream().filter(entry -> entry.getQuota() != null).count();
        if (withQuota == 0) {
            return;
        }
        if (withQuota != entries.size()) {
            throw new CourseOfferingValidationException(
                    "Either all audience entries have a quota or none");
        }
        if (entries.stream().anyMatch(entry -> entry.getKind().isExclusive())) {
            throw new CourseOfferingValidationException(
                    "The own-organization audience takes no quota");
        }
        int sum = entries.stream().mapToInt(CourseOfferingAudience::getQuota).sum();
        if (sum > offering.getMaxParticipants()) {
            throw new CourseOfferingValidationException(
                    "The quotas ("
                            + sum
                            + ") must not exceed the maximum number of participants ("
                            + offering.getMaxParticipants()
                            + ")");
        }
    }

    /**
     * Seats and waitlist places already carry an audience entry, so they pin the quota setup of a
     * published offering: the maximum may not drop below the current participants, an entry holding
     * places cannot be removed, and no quota may drop below its participants.
     */
    private void requireQuotasFitRegistrations(
            CourseOffering offering, Set<Long> removedAudienceIds) {
        if (offering.getId() == null || offering.getStatus() != CourseOfferingStatus.PUBLISHED) {
            return;
        }
        List<CourseRegistrationRepository.AudienceSeatCount> seats =
                registrationRepository.countSeatsByAudience(offering.getId());
        if (seats.isEmpty()) {
            return;
        }
        long participants =
                seats.stream()
                        .filter(row -> row.getStatus() == CourseRegistrationStatus.PARTICIPANT)
                        .mapToLong(CourseRegistrationRepository.AudienceSeatCount::getCount)
                        .sum();
        if (participants > offering.getMaxParticipants()) {
            throw new CourseOfferingValidationException(
                    "The maximum number of participants cannot be lower than the current"
                            + " participants ("
                            + participants
                            + ")");
        }
        Map<Long, Integer> quotas = new HashMap<>();
        offering.getAudiences()
                .forEach(
                        entry -> {
                            if (entry.getId() != null && entry.getQuota() != null) {
                                quotas.put(entry.getId(), entry.getQuota());
                            }
                        });
        for (CourseRegistrationRepository.AudienceSeatCount row : seats) {
            if (row.getAudienceId() != null && removedAudienceIds.contains(row.getAudienceId())) {
                throw new CourseOfferingValidationException(
                        "An audience entry with participants or waitlist places cannot be removed");
            }
            Integer quota = quotas.get(row.getAudienceId());
            if (row.getStatus() == CourseRegistrationStatus.PARTICIPANT
                    && quota != null
                    && row.getCount() > quota) {
                throw new CourseOfferingValidationException(
                        "A quota cannot be lower than the number of its participants");
            }
        }
    }

    private void validatePrerequisites(List<CoursePrerequisiteEntryRequest> entries) {
        Set<String> seen = new HashSet<>();
        for (CoursePrerequisiteEntryRequest entry : entries) {
            String configKey = entry.getConfigKey().trim();
            if (!seen.add(entry.getKind() + ":" + configKey)) {
                throw new CourseOfferingValidationException("Duplicate prerequisite");
            }
            boolean known =
                    switch (entry.getKind()) {
                        case TRAINING_COURSE ->
                                trainingCourseTypeRepository.findByConfigKey(configKey).isPresent();
                        case CERTIFICATION ->
                                certDefinitionRepository.findByConfigKey(configKey).isPresent();
                        case DRIVING_LICENSE ->
                                drivingLicenseDefinitionRepository
                                        .findByConfigKey(configKey)
                                        .isPresent();
                    };
            if (!known) {
                throw new CourseOfferingValidationException(
                        "Unknown prerequisite: " + entry.getKind() + " " + configKey);
            }
        }
    }

    private void validateAudience(Long tenantId, List<CourseAudienceEntryRequest> entries) {
        if (entries.stream().anyMatch(entry -> entry.getKind().isExclusive())
                && entries.size() > 1) {
            throw new CourseOfferingValidationException(
                    "The own-organization audience cannot be combined with other entries");
        }
        Set<String> seen = new HashSet<>();
        for (CourseAudienceEntryRequest entry : entries) {
            CourseAudienceKind kind = entry.getKind();
            if (kind.needsReference() != (entry.getRefId() != null)) {
                throw new CourseOfferingValidationException(
                        kind.needsReference()
                                ? "Audience " + kind + " needs a reference"
                                : "Audience " + kind + " takes no reference");
            }
            if (!seen.add(kind + ":" + entry.getRefId())) {
                throw new CourseOfferingValidationException("Duplicate audience entry");
            }
            if (entry.getQuota() != null && kind.isExclusive()) {
                throw new CourseOfferingValidationException(
                        "The own-organization audience takes no quota");
            }
            boolean known =
                    switch (kind) {
                        case ORG, ORG_SUBTREE ->
                                organizationRepository.existsByIdAndTenantId(
                                        entry.getRefId(), tenantId);
                        case ORG_TYPE ->
                                organizationTypeRepository
                                        .findById(entry.getRefId())
                                        .filter(type -> tenantId.equals(type.getTenantId()))
                                        .isPresent();
                        case OWN_ORG -> true;
                    };
            if (!known) {
                throw new CourseOfferingValidationException(
                        "Unknown audience reference: " + entry.getRefId());
            }
        }
    }

    private void validatePublishable(CourseOffering offering) {
        requireValidQuotas(offering);
        if (offering.getRegistrationStart() == null || offering.getRegistrationEnd() == null) {
            throw new CourseOfferingValidationException("Registration period is required");
        }
        if (offering.getSessions().isEmpty()) {
            throw new CourseOfferingValidationException("At least one session is required");
        }
        Instant firstStart =
                offering.getSessions().stream()
                        .map(CourseSession::getStart)
                        .min(Comparator.naturalOrder())
                        .orElseThrow();
        if (offering.getRegistrationEnd().isAfter(firstStart)) {
            throw new CourseOfferingValidationException(
                    "Registration must end before the first session starts");
        }
        if (offering.isHasExam()
                && offering.getSessions().stream()
                        .noneMatch(session -> session.getType() == CourseSessionType.EXAM)) {
            throw new CourseOfferingValidationException(
                    "An offering with an exam needs an exam session");
        }
    }

    private CourseOfferingDto save(CourseOffering offering) {
        offering.syncSchedule();
        CourseOffering saved = offeringRepository.saveAndFlush(offering);
        return toDto(saved, loadTypeNames(saved.getTenantId(), List.of(saved)));
    }

    private Map<Long, String> loadTypeNames(Long tenantId, List<CourseOffering> offerings) {
        List<Long> typeIds =
                offerings.stream().map(CourseOffering::getTrainingCourseTypeId).distinct().toList();
        return trainingCourseTypeRepository.findAllById(typeIds).stream()
                .filter(type -> tenantId.equals(type.getTenantId()))
                .collect(
                        Collectors.toMap(
                                TenantTrainingCourseType::getId,
                                TenantTrainingCourseType::getName,
                                (a, b) -> a));
    }

    private CourseOfferingDto toDto(CourseOffering offering, Map<Long, String> typeNames) {
        List<CourseSession> sessions =
                offering.getSessions().stream()
                        .sorted(
                                Comparator.comparing(CourseSession::getStart)
                                        .thenComparing(
                                                CourseSession::getId,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
        return CourseOfferingDto.builder()
                .id(offering.getId())
                .organizationId(offering.getOrganizationId())
                .trainingCourseTypeId(offering.getTrainingCourseTypeId())
                .trainingCourseTypeName(typeNames.get(offering.getTrainingCourseTypeId()))
                .content(offering.getContent())
                .notes(offering.getNotes())
                .templateName(offering.getTemplateName())
                .status(offering.getStatus())
                .registrationStart(offering.getRegistrationStart())
                .registrationEnd(offering.getRegistrationEnd())
                .maxParticipants(offering.getMaxParticipants())
                .hasExam(offering.isHasExam())
                .requiresHomeOrgApproval(offering.isRequiresHomeOrgApproval())
                .firstSessionStart(offering.getFirstSessionStart())
                .lastSessionEnd(offering.getLastSessionEnd())
                .sessions(sessions.stream().map(this::toSessionDto).toList())
                .prerequisites(toPrerequisitesDto(offering))
                .cancelledAt(offering.getCancelledAt())
                .cancelReason(offering.getCancelReason())
                .audiences(
                        offering.getAudiences().stream()
                                .map(
                                        audience ->
                                                CourseAudienceEntryDto.builder()
                                                        .id(audience.getId())
                                                        .kind(audience.getKind())
                                                        .refId(audience.getRefId())
                                                        .quota(audience.getQuota())
                                                        .build())
                                .toList())
                .build();
    }

    private CoursePrerequisitesDto toPrerequisitesDto(CourseOffering offering) {
        return CoursePrerequisitesDto.builder()
                .trainingCourseRequirementMode(offering.getTrainingCourseRequirementMode())
                .certRequirementMode(offering.getCertRequirementMode())
                .drivingLicenseRequirementMode(offering.getDrivingLicenseRequirementMode())
                .entries(
                        offering.getPrerequisites().stream()
                                .map(
                                        prerequisite ->
                                                CoursePrerequisitesDto.Entry.builder()
                                                        .kind(prerequisite.getKind())
                                                        .configKey(prerequisite.getConfigKey())
                                                        .build())
                                .toList())
                .build();
    }

    private CourseSessionDto toSessionDto(CourseSession session) {
        return CourseSessionDto.of(session);
    }

    private String resolveTypeName(CourseOffering offering) {
        return loadTypeNames(offering.getTenantId(), List.of(offering))
                .getOrDefault(offering.getTrainingCourseTypeId(), "");
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
