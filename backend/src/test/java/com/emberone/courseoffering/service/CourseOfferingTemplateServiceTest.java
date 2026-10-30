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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingPrerequisite;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CoursePrerequisiteKind;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.domain.CourseSessionType;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.qualification.domain.QualificationRequirementMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseOfferingTemplateServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long SOURCE_ID = 100L;

    // Berlin: 2027-03-22 is CET (+01:00), 2027-04-05 is CEST (+02:00).
    private static final Instant FIRST = Instant.parse("2027-03-22T07:00:00Z"); // 08:00 local
    private static final Instant SECOND = Instant.parse("2027-03-24T14:00:00Z"); // 15:00 local

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseOfferingCatalogService catalogService;
    @Mock CourseOfferingService offeringService;

    @InjectMocks CourseOfferingTemplateService service;

    @BeforeEach
    void setUp() {
        lenient()
                .when(offeringRepository.saveAndFlush(any(CourseOffering.class)))
                .thenAnswer(
                        invocation -> {
                            CourseOffering saved = invocation.getArgument(0);
                            saved.setId(200L);
                            return saved;
                        });
    }

    @Test
    void saveAsTemplateCopiesEverythingUnshifted() {
        CourseOffering source = offering(CourseOfferingStatus.PUBLISHED);
        stubFind(source);

        service.saveAsTemplate(ORG_ID, SOURCE_ID, "  Atemschutz Grundlehrgang ");

        CourseOffering template = savedOffering();
        assertThat(template.getStatus()).isEqualTo(CourseOfferingStatus.TEMPLATE);
        assertThat(template.getTemplateName()).isEqualTo("Atemschutz Grundlehrgang");

        assertThat(template.getMaxParticipants()).isEqualTo(12);
        assertThat(template.getTrainingCourseRequirementMode())
                .isEqualTo(QualificationRequirementMode.ANY);
        assertThat(template.getRegistrationEnd()).isEqualTo(source.getRegistrationEnd());
        assertThat(template.getSessions())
                .extracting(CourseSession::getStart)
                .containsExactly(FIRST, SECOND);
        assertThat(template.getAudiences()).hasSize(1);
        assertThat(template.getPrerequisites()).hasSize(1);
        assertThat(template.getFirstSessionStart()).isEqualTo(FIRST);
    }

    @Test
    void aTemplateCannotBeSavedAsTemplate() {
        stubFind(offering(CourseOfferingStatus.TEMPLATE));

        assertThatThrownBy(() -> service.saveAsTemplate(ORG_ID, SOURCE_ID, "Copy"))
                .isInstanceOf(CourseOfferingValidationException.class);
        verify(offeringRepository, never()).saveAndFlush(any());
    }

    @Test
    void instantiateMovesSessionsAndRegistrationByCalendarDaysKeepingLocalTime() {
        CourseOffering template = offering(CourseOfferingStatus.TEMPLATE);
        stubFind(template);

        service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 4, 5));

        CourseOffering draft = savedOffering();
        assertThat(draft.getStatus()).isEqualTo(CourseOfferingStatus.DRAFT);
        assertThat(draft.getTemplateName()).isNull();
        // +14 days across the change to summer time: local 08:00 / 15:00 stay, UTC moves.
        assertThat(draft.getSessions())
                .extracting(CourseSession::getStart)
                .containsExactly(
                        Instant.parse("2027-04-05T06:00:00Z"),
                        Instant.parse("2027-04-07T13:00:00Z"));
        assertThat(draft.getRegistrationStart()).isEqualTo(Instant.parse("2027-03-06T11:00:00Z"));
        assertThat(draft.getRegistrationEnd()).isEqualTo(Instant.parse("2027-04-04T06:00:00Z"));
        assertThat(draft.getFirstSessionStart()).isEqualTo(Instant.parse("2027-04-05T06:00:00Z"));
        assertThat(draft.getAudiences()).hasSize(1);
        assertThat(draft.getPrerequisites()).hasSize(1);
    }

    @Test
    void instantiateTwiceYieldsIndependentDrafts() {
        CourseOffering template = offering(CourseOfferingStatus.TEMPLATE);
        stubFind(template);

        service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 6, 1));
        service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 9, 1));

        assertThat(template.getStatus()).isEqualTo(CourseOfferingStatus.TEMPLATE);
        assertThat(template.getSessions())
                .extracting(CourseSession::getStart)
                .first()
                .isEqualTo(FIRST);
        verify(offeringRepository, org.mockito.Mockito.times(2)).saveAndFlush(any());
    }

    @Test
    void instantiateRequiresATemplate() {
        stubFind(offering(CourseOfferingStatus.DRAFT));

        assertThatThrownBy(() -> service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 6, 1)))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void instantiateRequiresTheCourseTypeToStillBeOfferable() {
        stubFind(offering(CourseOfferingStatus.TEMPLATE));
        doThrow(new ConflictException("not offerable"))
                .when(catalogService)
                .requireOfferable(5L, ORG_ID);

        assertThatThrownBy(() -> service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 6, 1)))
                .isInstanceOf(ConflictException.class);
        verify(offeringRepository, never()).saveAndFlush(any());
    }

    @Test
    void templateWithoutSessionsIsCopiedUnshifted() {
        CourseOffering template = offering(CourseOfferingStatus.TEMPLATE);
        template.getSessions().clear();
        stubFind(template);

        service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 6, 1));

        CourseOffering draft = savedOffering();
        assertThat(draft.getSessions()).isEmpty();
        assertThat(draft.getRegistrationEnd()).isEqualTo(template.getRegistrationEnd());
    }

    @Test
    void quotasAreCopiedToTemplateAndToDraft() {
        CourseOffering source = offering(CourseOfferingStatus.PUBLISHED);
        source.getAudiences().getFirst().setQuota(7);
        source.getAudiences()
                .add(
                        CourseOfferingAudience.builder()
                                .tenantId(TENANT_ID)
                                .offering(source)
                                .kind(CourseAudienceKind.ORG)
                                .refId(21L)
                                .quota(5)
                                .build());
        stubFind(source);

        service.saveAsTemplate(ORG_ID, SOURCE_ID, "Vorlage");
        CourseOffering template = savedOffering();
        assertThat(template.getAudiences())
                .extracting(CourseOfferingAudience::getQuota)
                .containsExactly(7, 5);

        stubFind(template);
        service.instantiate(ORG_ID, SOURCE_ID, LocalDate.of(2027, 6, 1));
        assertThat(savedOffering().getAudiences())
                .extracting(CourseOfferingAudience::getQuota)
                .containsExactly(7, 5);
    }

    private CourseOffering savedOffering() {
        ArgumentCaptor<CourseOffering> captor = ArgumentCaptor.forClass(CourseOffering.class);
        verify(offeringRepository, org.mockito.Mockito.atLeastOnce())
                .saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void stubFind(CourseOffering offering) {
        when(offeringService.requireOffering(ORG_ID, SOURCE_ID)).thenReturn(offering);
    }

    private static CourseOffering offering(CourseOfferingStatus status) {
        CourseOffering offering =
                CourseOffering.builder()
                        .id(SOURCE_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(ORG_ID)
                        .trainingCourseTypeId(5L)
                        .templateName(status == CourseOfferingStatus.TEMPLATE ? "Vorlage" : null)
                        .status(status)
                        .registrationStart(Instant.parse("2027-02-20T11:00:00Z"))
                        .registrationEnd(Instant.parse("2027-03-21T07:00:00Z"))
                        .maxParticipants(12)
                        .trainingCourseRequirementMode(QualificationRequirementMode.ANY)
                        .sessions(new ArrayList<>())
                        .audiences(new ArrayList<>())
                        .prerequisites(new ArrayList<>())
                        .build();
        offering.getSessions().add(session(offering, CourseSessionType.THEORY, FIRST));
        offering.getSessions().add(session(offering, CourseSessionType.EXAM, SECOND));
        offering.getAudiences()
                .add(
                        CourseOfferingAudience.builder()
                                .tenantId(TENANT_ID)
                                .offering(offering)
                                .kind(CourseAudienceKind.ORG_SUBTREE)
                                .refId(20L)
                                .build());
        offering.getPrerequisites()
                .add(
                        CourseOfferingPrerequisite.builder()
                                .tenantId(TENANT_ID)
                                .offering(offering)
                                .kind(CoursePrerequisiteKind.TRAINING_COURSE)
                                .configKey("AGT")
                                .build());
        return offering;
    }

    private static CourseSession session(
            CourseOffering offering, CourseSessionType type, Instant start) {
        return CourseSession.builder()
                .tenantId(TENANT_ID)
                .offering(offering)
                .type(type)
                .start(start)
                .end(start.plusSeconds(7200))
                .build();
    }
}
