/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingPrerequisite;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CoursePrerequisiteKind;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.domain.CourseSessionType;
import com.emberone.courseoffering.dto.CourseAudienceEntryRequest;
import com.emberone.courseoffering.dto.CourseAudienceQuotaRequest;
import com.emberone.courseoffering.dto.CourseOfferingDto;
import com.emberone.courseoffering.dto.CoursePrerequisiteEntryRequest;
import com.emberone.courseoffering.dto.CourseSessionRequest;
import com.emberone.courseoffering.dto.CreateCourseOfferingRequest;
import com.emberone.courseoffering.dto.ReplaceCoursePrerequisitesRequest;
import com.emberone.courseoffering.dto.UpdateCourseOfferingRequest;
import com.emberone.courseoffering.event.CourseOfferingSessionsChangedEvent;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.repository.OrganizationTypeDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TokenService;
import com.emberone.qualification.domain.QualificationRequirementMode;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CourseOfferingServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long OFFERING_ID = 100L;
    private static final Instant COURSE_START = Instant.parse("2027-03-01T08:00:00Z");

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseOfferingCatalogService catalogService;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    @Mock TenantCertDefinitionRepository certDefinitionRepository;
    @Mock TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationTypeDefinitionRepository organizationTypeRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CourseOfferingService service;

    @BeforeEach
    void setUp() {
        lenient().when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(5L);
        type.setTenantId(TENANT_ID);
        type.setName("Atemschutz");
        lenient().when(trainingCourseTypeRepository.findAllById(any())).thenReturn(List.of(type));
        lenient()
                .when(offeringRepository.saveAndFlush(any(CourseOffering.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createRejectsTypeTheOrganizationMayNotOffer() {
        doThrow(new ConflictException("not offerable"))
                .when(catalogService)
                .requireOfferable(5L, ORG_ID);
        CreateCourseOfferingRequest request = new CreateCourseOfferingRequest();
        request.setTrainingCourseTypeId(5L);

        request.setMaxParticipants(10);

        assertThatThrownBy(() -> service.create(ORG_ID, request))
                .isInstanceOf(ConflictException.class);
        verify(offeringRepository, never()).saveAndFlush(any());
    }

    @Test
    void createStoresDraftWithTrimmedTopic() {
        CreateCourseOfferingRequest request = new CreateCourseOfferingRequest();
        request.setTrainingCourseTypeId(5L);

        request.setMaxParticipants(10);

        CourseOfferingDto dto = service.create(ORG_ID, request);

        assertThat(dto.getStatus()).isEqualTo(CourseOfferingStatus.DRAFT);

        assertThat(dto.getOrganizationId()).isEqualTo(ORG_ID);
    }

    @Test
    void createRejectsRegistrationStartAfterEnd() {
        CreateCourseOfferingRequest request = new CreateCourseOfferingRequest();
        request.setTrainingCourseTypeId(5L);

        request.setMaxParticipants(10);
        request.setRegistrationStart(COURSE_START);
        request.setRegistrationEnd(COURSE_START.minus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.create(ORG_ID, request))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void publishRequiresSessions() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        assertThatThrownBy(() -> service.publish(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("session");
    }

    @Test
    void publishRequiresRegistrationToEndBeforeFirstSession() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        offering.setRegistrationEnd(COURSE_START.plus(1, ChronoUnit.HOURS));
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        stubFind(offering);

        assertThatThrownBy(() -> service.publish(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("first session");
    }

    @Test
    void publishWithExamRequiresExamSession() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, true);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        stubFind(offering);

        assertThatThrownBy(() -> service.publish(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("exam");
    }

    @Test
    void publishMovesDraftToPublished() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, true);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        addSession(offering, 2L, CourseSessionType.EXAM, COURSE_START.plus(7, ChronoUnit.DAYS));
        stubFind(offering);

        CourseOfferingDto dto = service.publish(ORG_ID, OFFERING_ID);

        assertThat(dto.getStatus()).isEqualTo(CourseOfferingStatus.PUBLISHED);
        assertThat(dto.getFirstSessionStart()).isEqualTo(COURSE_START);
        assertThat(dto.getLastSessionEnd())
                .isEqualTo(COURSE_START.plus(7, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
    }

    @Test
    void publishedOfferingKeepsItsLastSession() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        stubFind(offering);

        assertThatThrownBy(() -> service.deleteSession(ORG_ID, OFFERING_ID, 1L))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void publishAllowsPublicOfferingWithoutAudienceRows() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        offering.setRegistrationStart(Instant.parse("2026-01-01T00:00:00Z"));
        offering.setRegistrationEnd(Instant.parse("2026-01-15T00:00:00Z"));
        stubFind(offering);

        CourseOfferingDto dto = service.publish(ORG_ID, OFFERING_ID);

        assertThat(dto.getStatus()).isEqualTo(CourseOfferingStatus.PUBLISHED);
        assertThat(offering.getAudiences()).isEmpty();
    }

    @Test
    void scheduleIsCachedFromSessions() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        CourseOfferingDto dto = service.addSession(ORG_ID, OFFERING_ID, sessionRequest());

        assertThat(dto.getFirstSessionStart()).isEqualTo(COURSE_START);
        assertThat(offering.getLastSessionEnd()).isEqualTo(COURSE_START.plus(2, ChronoUnit.HOURS));
    }

    @Test
    void audienceCombinesOrgTypeAndSubtreeEntries() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        when(organizationRepository.existsByIdAndTenantId(20L, TENANT_ID)).thenReturn(true);
        when(organizationRepository.existsByIdAndTenantId(21L, TENANT_ID)).thenReturn(true);
        com.emberone.organization.domain.OrganizationTypeDefinition type =
                new com.emberone.organization.domain.OrganizationTypeDefinition();
        type.setTenantId(TENANT_ID);
        when(organizationTypeRepository.findById(30L)).thenReturn(Optional.of(type));

        CourseOfferingDto dto =
                service.replaceAudiences(
                        ORG_ID,
                        OFFERING_ID,
                        List.of(
                                audience(CourseAudienceKind.ORG_SUBTREE, 20L),
                                audience(CourseAudienceKind.ORG_SUBTREE, 21L),
                                audience(CourseAudienceKind.ORG_TYPE, 30L),
                                audience(CourseAudienceKind.ORG, 20L)));

        assertThat(dto.getAudiences()).hasSize(4);
    }

    @Test
    void ownOrgAudienceCannotBeCombined() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(
                                                audience(CourseAudienceKind.OWN_ORG, null),
                                                audience(CourseAudienceKind.ORG_TYPE, 30L))))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void audienceReferenceRulesFollowTheKind() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(audience(CourseAudienceKind.ORG, null))))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(audience(CourseAudienceKind.OWN_ORG, 5L))))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void audienceRejectsDuplicatesAndUnknownReferences() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        when(organizationRepository.existsByIdAndTenantId(20L, TENANT_ID)).thenReturn(true);
        when(organizationRepository.existsByIdAndTenantId(99L, TENANT_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(
                                                audience(CourseAudienceKind.ORG, 20L),
                                                audience(CourseAudienceKind.ORG, 20L))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("Duplicate");
        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(audience(CourseAudienceKind.ORG_SUBTREE, 99L))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void prerequisitesStoreModesAndEntries() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        CourseOfferingPrerequisite kept =
                CourseOfferingPrerequisite.builder()
                        .id(11L)
                        .tenantId(TENANT_ID)
                        .offering(offering)
                        .kind(CoursePrerequisiteKind.TRAINING_COURSE)
                        .configKey("AGT")
                        .build();
        offering.getPrerequisites().add(kept);
        stubFind(offering);
        when(trainingCourseTypeRepository.findByConfigKey("AGT"))
                .thenReturn(Optional.of(new TenantTrainingCourseType()));
        when(certDefinitionRepository.findByConfigKey("G26"))
                .thenReturn(Optional.of(new TenantCertDefinition()));

        CourseOfferingDto dto =
                service.replacePrerequisites(
                        ORG_ID,
                        OFFERING_ID,
                        prerequisites(
                                QualificationRequirementMode.ANY,
                                prerequisite(CoursePrerequisiteKind.TRAINING_COURSE, "AGT"),
                                prerequisite(CoursePrerequisiteKind.CERTIFICATION, "G26")));

        assertThat(dto.getPrerequisites().getTrainingCourseRequirementMode())
                .isEqualTo(QualificationRequirementMode.ANY);
        assertThat(dto.getPrerequisites().getEntries()).hasSize(2);
        assertThat(offering.getPrerequisites()).contains(kept);
        assertThat(offering.getPrerequisites()).hasSize(2);
        verify(offeringRepository, never()).flush();
    }

    @Test
    void prerequisitesRejectUnknownAndDuplicateKeys() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        when(trainingCourseTypeRepository.findByConfigKey("AGT"))
                .thenReturn(Optional.of(new TenantTrainingCourseType()));
        when(drivingLicenseDefinitionRepository.findByConfigKey("C")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.replacePrerequisites(
                                        ORG_ID,
                                        OFFERING_ID,
                                        prerequisites(
                                                QualificationRequirementMode.ALL,
                                                prerequisite(
                                                        CoursePrerequisiteKind.TRAINING_COURSE,
                                                        "AGT"),
                                                prerequisite(
                                                        CoursePrerequisiteKind.TRAINING_COURSE,
                                                        "AGT"))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("Duplicate");
        assertThatThrownBy(
                        () ->
                                service.replacePrerequisites(
                                        ORG_ID,
                                        OFFERING_ID,
                                        prerequisites(
                                                QualificationRequirementMode.ALL,
                                                prerequisite(
                                                        CoursePrerequisiteKind.DRIVING_LICENSE,
                                                        "C"))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void closedOfferingCannotBeChanged() {
        CourseOffering offering = offering(CourseOfferingStatus.COMPLETED, false);
        stubFind(offering);

        assertThatThrownBy(() -> service.addSession(ORG_ID, OFFERING_ID, sessionRequest()))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void onlyDraftsCanBeDeleted() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        stubFind(offering);

        assertThatThrownBy(() -> service.delete(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
        verify(offeringRepository, never()).delete(any(CourseOffering.class));
    }

    @Test
    void templatesCannotBePublished() {
        stubFind(offering(CourseOfferingStatus.TEMPLATE, false));
        assertThatThrownBy(() -> service.publish(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void templatesCanBeDeleted() {
        CourseOffering template = offering(CourseOfferingStatus.TEMPLATE, false);
        stubFind(template);

        service.delete(ORG_ID, OFFERING_ID);

        verify(offeringRepository).delete(template);
    }

    @Test
    void sessionStartMustBeBeforeEnd() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        CourseSessionRequest request = sessionRequest();
        request.setEnd(request.getStart());

        assertThatThrownBy(() -> service.addSession(ORG_ID, OFFERING_ID, request))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    // --- session change notifications ---

    @Test
    void sessionChangeOfPublishedOfferingNotifiesParticipantsAndWaitlistOnce() {
        CourseOffering offering = publishedWithSession();
        stubFind(offering);
        when(registrationRepository.findUserIdsByOfferingAndStatusIn(
                        OFFERING_ID,
                        List.of(
                                CourseRegistrationStatus.PARTICIPANT,
                                CourseRegistrationStatus.WAITLIST)))
                .thenReturn(List.of(3L, 4L));
        CourseSessionRequest request = sessionRequest();
        request.setStart(COURSE_START.plus(1, ChronoUnit.DAYS));
        request.setEnd(COURSE_START.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));

        service.updateSession(ORG_ID, OFFERING_ID, 1L, request);

        ArgumentCaptor<CourseOfferingSessionsChangedEvent> captor =
                ArgumentCaptor.forClass(CourseOfferingSessionsChangedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        CourseOfferingSessionsChangedEvent event = captor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.offeringId()).isEqualTo(OFFERING_ID);
        assertThat(event.trainingCourseTypeName()).isEqualTo("Atemschutz");
        assertThat(event.userIds()).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    void addingOrDeletingASessionNotifies() {
        CourseOffering offering = publishedWithSession();
        stubFind(offering);
        when(registrationRepository.findUserIdsByOfferingAndStatusIn(any(), any()))
                .thenReturn(List.of(3L));

        service.addSession(ORG_ID, OFFERING_ID, sessionRequest());
        service.deleteSession(ORG_ID, OFFERING_ID, 1L);

        verify(eventPublisher, times(2))
                .publishEvent(any(CourseOfferingSessionsChangedEvent.class));
    }

    @Test
    void sessionChangeWithoutScheduleChangeDoesNotNotify() {
        CourseOffering offering = publishedWithSession();
        stubFind(offering);
        CourseSessionRequest request = sessionRequest();
        request.setNotes("Bitte Schutzkleidung mitbringen");

        service.updateSession(ORG_ID, OFFERING_ID, 1L, request);

        verify(eventPublisher, never()).publishEvent(any());
        verify(registrationRepository, never()).findUserIdsByOfferingAndStatusIn(any(), any());
    }

    @Test
    void sessionChangeWithoutParticipantsDoesNotNotify() {
        CourseOffering offering = publishedWithSession();
        stubFind(offering);
        when(registrationRepository.findUserIdsByOfferingAndStatusIn(any(), any()))
                .thenReturn(List.of());

        service.addSession(ORG_ID, OFFERING_ID, sessionRequest());

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sessionChangeOfDraftDoesNotNotify() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        service.addSession(ORG_ID, OFFERING_ID, sessionRequest());

        verify(eventPublisher, never()).publishEvent(any());
    }

    private CourseOffering publishedWithSession() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        offering.syncSchedule();
        return offering;
    }

    // --- quotas ---

    @Test
    void quotasMustNotExceedTheMaximum() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        knownOrganizations();

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(
                                                audience(CourseAudienceKind.ORG, 20L, 6),
                                                audience(CourseAudienceKind.ORG_SUBTREE, 21L, 5))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("exceed");
    }

    @Test
    void quotasAreStoredWhenTheyAddUp() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        knownOrganizations();

        CourseOfferingDto dto =
                service.replaceAudiences(
                        ORG_ID,
                        OFFERING_ID,
                        List.of(
                                audience(CourseAudienceKind.ORG, 20L, 3),
                                audience(CourseAudienceKind.ORG_SUBTREE, 21L, 7)));

        assertThat(dto.getAudiences())
                .extracting(com.emberone.courseoffering.dto.CourseAudienceEntryDto::getQuota)
                .containsExactlyInAnyOrder(3, 7);
    }

    @Test
    void eitherAllEntriesHaveAQuotaOrNone() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);
        knownOrganizations();

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(
                                                audience(CourseAudienceKind.ORG, 20L, 10),
                                                audience(CourseAudienceKind.ORG_SUBTREE, 21L))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("all audience entries");
    }

    @Test
    void ownOrgAudienceTakesNoQuota() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(audience(CourseAudienceKind.OWN_ORG, null, 10))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("no quota");
    }

    @Test
    void loweringTheMaximumBelowTheQuotasIsRejected() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);

        assertThatThrownBy(() -> service.update(ORG_ID, OFFERING_ID, updateRequest(9)))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("exceed");
    }

    @Test
    void maximumAndQuotasCanChangeInOneStep() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);
        UpdateCourseOfferingRequest request = updateRequest(12);
        request.setAudienceQuotas(List.of(quota(1L, 5), quota(2L, 7)));

        service.update(ORG_ID, OFFERING_ID, request);

        assertThat(offering.getMaxParticipants()).isEqualTo(12);
        assertThat(offering.getAudiences())
                .extracting(CourseOfferingAudience::getQuota)
                .containsExactly(5, 7);
    }

    @Test
    void emptyQuotaListRemovesAllQuotas() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);
        UpdateCourseOfferingRequest request = updateRequest(12);
        request.setAudienceQuotas(List.of());

        service.update(ORG_ID, OFFERING_ID, request);

        assertThat(offering.getAudiences())
                .extracting(CourseOfferingAudience::getQuota)
                .containsOnlyNulls();
    }

    @Test
    void quotaRequestRejectsUnknownEntriesAndDuplicates() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 10);
        stubFind(offering);
        UpdateCourseOfferingRequest unknown = updateRequest(10);
        unknown.setAudienceQuotas(List.of(quota(99L, 10)));
        UpdateCourseOfferingRequest duplicate = updateRequest(10);
        duplicate.setAudienceQuotas(List.of(quota(1L, 5), quota(1L, 5)));

        assertThatThrownBy(() -> service.update(ORG_ID, OFFERING_ID, unknown))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThatThrownBy(() -> service.update(ORG_ID, OFFERING_ID, duplicate))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void createRefusesQuotas() {
        CreateCourseOfferingRequest request = new CreateCourseOfferingRequest();
        request.setTrainingCourseTypeId(5L);

        request.setMaxParticipants(10);
        request.setAudienceQuotas(List.of(quota(1L, 10)));

        assertThatThrownBy(() -> service.create(ORG_ID, request))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void publishRevalidatesQuotas() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 11);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        stubFind(offering);

        assertThatThrownBy(() -> service.publish(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("exceed");
    }

    @Test
    void publishedOfferingKeepsValidQuotaSetupWhenAudienceChanges() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);
        knownOrganizations();

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(audience(CourseAudienceKind.ORG, 20L, 11))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("exceed");
    }

    @Test
    void replacingTheAudienceKeepsIdsOfKeptEntries() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        CourseOfferingAudience kept =
                entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, null);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, null);
        stubFind(offering);
        knownOrganizations();

        service.replaceAudiences(
                ORG_ID,
                OFFERING_ID,
                List.of(
                        audience(CourseAudienceKind.ORG, 20L, 6),
                        audience(CourseAudienceKind.ORG_TYPE, 30L, 4)));

        assertThat(offering.getAudiences()).hasSize(2).contains(kept);
        assertThat(kept.getQuota()).isEqualTo(6);
    }

    @Test
    void quotasCanBeSwitchedOnWhileSeatsFit() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        CourseOfferingAudience entry =
                entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, null);
        stubFind(offering);
        knownOrganizations();
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(List.of(seats(1L, CourseRegistrationStatus.PARTICIPANT, 2)));

        service.replaceAudiences(
                ORG_ID, OFFERING_ID, List.of(audience(CourseAudienceKind.ORG, 20L, 10)));

        assertThat(entry.getQuota()).isEqualTo(10);
    }

    @Test
    void anEntryHoldingPlacesCannotBeRemoved() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);
        knownOrganizations();
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(List.of(seats(2L, CourseRegistrationStatus.WAITLIST, 1)));

        assertThatThrownBy(
                        () ->
                                service.replaceAudiences(
                                        ORG_ID,
                                        OFFERING_ID,
                                        List.of(audience(CourseAudienceKind.ORG, 20L, 10))))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("cannot be removed");
    }

    @Test
    void aQuotaCannotDropBelowItsParticipants() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(List.of(seats(1L, CourseRegistrationStatus.PARTICIPANT, 4)));
        UpdateCourseOfferingRequest request = updateRequest(10);
        request.setAudienceQuotas(List.of(quota(1L, 3), quota(2L, 7)));

        assertThatThrownBy(() -> service.update(ORG_ID, OFFERING_ID, request))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("lower than");
    }

    @Test
    void theMaximumCannotDropBelowTheParticipantsWithoutQuotas() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, null);
        stubFind(offering);
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(
                        List.of(
                                seats(null, CourseRegistrationStatus.PARTICIPANT, 5),
                                seats(null, CourseRegistrationStatus.WAITLIST, 3)));

        assertThatThrownBy(() -> service.update(ORG_ID, OFFERING_ID, updateRequest(4)))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("cannot be lower than the current participants");
        service.update(ORG_ID, OFFERING_ID, updateRequest(5));
        assertThat(offering.getMaxParticipants()).isEqualTo(5);
    }

    @Test
    void rebalancingQuotasAboveTheirParticipantsIsAllowed() {
        CourseOffering offering = offering(CourseOfferingStatus.PUBLISHED, false);
        addSession(offering, 1L, CourseSessionType.THEORY, COURSE_START);
        entryWithQuota(offering, 1L, CourseAudienceKind.ORG, 20L, 4);
        entryWithQuota(offering, 2L, CourseAudienceKind.ORG_SUBTREE, 21L, 6);
        stubFind(offering);
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(List.of(seats(1L, CourseRegistrationStatus.PARTICIPANT, 4)));
        UpdateCourseOfferingRequest request = updateRequest(12);
        request.setAudienceQuotas(List.of(quota(1L, 5), quota(2L, 7)));

        service.update(ORG_ID, OFFERING_ID, request);

        assertThat(offering.getMaxParticipants()).isEqualTo(12);
    }

    @Test
    void changesTakeTheOfferingLockBeforeReadingTheQuotas() {
        CourseOffering offering = offering(CourseOfferingStatus.DRAFT, false);
        stubFind(offering);

        service.update(ORG_ID, OFFERING_ID, updateRequest(10));

        verify(offeringRepository).findForUpdate(OFFERING_ID);
    }

    private static CourseAudienceQuotaRequest quota(Long audienceId, Integer quota) {
        CourseAudienceQuotaRequest request = new CourseAudienceQuotaRequest();
        request.setAudienceId(audienceId);
        request.setQuota(quota);
        return request;
    }

    private void stubFind(CourseOffering offering) {
        lenient()
                .when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        // audience and field changes read the offering under its row lock
        lenient()
                .when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenReturn(Optional.of(offering));
    }

    private static CoursePrerequisiteEntryRequest prerequisite(
            CoursePrerequisiteKind kind, String configKey) {
        CoursePrerequisiteEntryRequest entry = new CoursePrerequisiteEntryRequest();
        entry.setKind(kind);
        entry.setConfigKey(configKey);
        return entry;
    }

    private static ReplaceCoursePrerequisitesRequest prerequisites(
            QualificationRequirementMode mode, CoursePrerequisiteEntryRequest... entries) {
        ReplaceCoursePrerequisitesRequest request = new ReplaceCoursePrerequisitesRequest();
        request.setTrainingCourseRequirementMode(mode);
        request.setCertRequirementMode(QualificationRequirementMode.ALL);
        request.setDrivingLicenseRequirementMode(QualificationRequirementMode.ALL);
        request.setEntries(List.of(entries));
        return request;
    }

    private static CourseAudienceEntryRequest audience(CourseAudienceKind kind, Long refId) {
        CourseAudienceEntryRequest entry = new CourseAudienceEntryRequest();
        entry.setKind(kind);
        entry.setRefId(refId);
        return entry;
    }

    private static CourseAudienceEntryRequest audience(
            CourseAudienceKind kind, Long refId, Integer quota) {
        CourseAudienceEntryRequest entry = audience(kind, refId);
        entry.setQuota(quota);
        return entry;
    }

    private static CourseOfferingAudience entryWithQuota(
            CourseOffering offering, long id, CourseAudienceKind kind, Long refId, Integer quota) {
        CourseOfferingAudience entry =
                CourseOfferingAudience.builder()
                        .id(id)
                        .tenantId(TENANT_ID)
                        .offering(offering)
                        .kind(kind)
                        .refId(refId)
                        .quota(quota)
                        .build();
        offering.getAudiences().add(entry);
        return entry;
    }

    private static UpdateCourseOfferingRequest updateRequest(int maxParticipants) {
        UpdateCourseOfferingRequest request = new UpdateCourseOfferingRequest();

        request.setMaxParticipants(maxParticipants);
        request.setRegistrationStart(COURSE_START.minus(30, ChronoUnit.DAYS));
        request.setRegistrationEnd(COURSE_START.minus(1, ChronoUnit.DAYS));
        return request;
    }

    private static CourseRegistrationRepository.AudienceSeatCount seats(
            Long audienceId, CourseRegistrationStatus status, long count) {
        return new CourseRegistrationRepository.AudienceSeatCount() {
            @Override
            public Long getAudienceId() {
                return audienceId;
            }

            @Override
            public CourseRegistrationStatus getStatus() {
                return status;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private void knownOrganizations() {
        lenient().when(organizationRepository.existsByIdAndTenantId(any(), any())).thenReturn(true);
        com.emberone.organization.domain.OrganizationTypeDefinition type =
                new com.emberone.organization.domain.OrganizationTypeDefinition();
        type.setTenantId(TENANT_ID);
        lenient().when(organizationTypeRepository.findById(any())).thenReturn(Optional.of(type));
    }

    private static void addAudience(CourseOffering offering, CourseAudienceKind kind, Long refId) {
        offering.getAudiences()
                .add(
                        CourseOfferingAudience.builder()
                                .tenantId(TENANT_ID)
                                .offering(offering)
                                .kind(kind)
                                .refId(refId)
                                .build());
    }

    private static CourseSessionRequest sessionRequest() {
        CourseSessionRequest request = new CourseSessionRequest();
        request.setType(CourseSessionType.THEORY);
        request.setStart(COURSE_START);
        request.setEnd(COURSE_START.plus(2, ChronoUnit.HOURS));
        return request;
    }

    private static CourseOffering offering(CourseOfferingStatus status, boolean hasExam) {
        return CourseOffering.builder()
                .id(OFFERING_ID)
                .tenantId(TENANT_ID)
                .organizationId(ORG_ID)
                .trainingCourseTypeId(5L)
                .status(status)
                .registrationStart(COURSE_START.minus(30, ChronoUnit.DAYS))
                .registrationEnd(COURSE_START.minus(1, ChronoUnit.DAYS))
                .maxParticipants(10)
                .hasExam(hasExam)
                .sessions(new ArrayList<>())
                .audiences(new ArrayList<>())
                .prerequisites(new ArrayList<>())
                .build();
    }

    private static void addSession(
            CourseOffering offering, Long id, CourseSessionType type, Instant start) {
        offering.getSessions()
                .add(
                        CourseSession.builder()
                                .id(id)
                                .tenantId(TENANT_ID)
                                .offering(offering)
                                .type(type)
                                .start(start)
                                .end(start.plus(2, ChronoUnit.HOURS))
                                .build());
    }
}
