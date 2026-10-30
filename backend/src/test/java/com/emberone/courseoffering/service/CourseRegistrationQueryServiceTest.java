/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.dto.CourseRegistrationPageDto;
import com.emberone.courseoffering.dto.CourseRegistrationSummaryDto;
import com.emberone.courseoffering.dto.SelfCourseRegistrationDto;
import com.emberone.courseoffering.dto.SelfCourseRegistrationPageDto;
import com.emberone.courseoffering.dto.UserCourseRegistrationDto;
import com.emberone.courseoffering.dto.UserCourseRegistrationPageDto;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseRegistrationNotFoundException;
import com.emberone.courseoffering.list.CourseRegistrationListFilter;
import com.emberone.courseoffering.list.UserCourseRegistrationListFilter;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationQueryServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long OFFERING_ID = 100L;

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock UserRepository userRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock TokenService tokenService;

    CourseRegistrationQueryService service;
    CourseOffering offering;

    @BeforeEach
    void setUp() {
        lenient().when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        service =
                new CourseRegistrationQueryService(
                        offeringRepository,
                        registrationRepository,
                        new CourseRegistrationAssembler(
                                registrationRepository,
                                userRepository,
                                new AppNavigationResolver()),
                        trainingCourseTypeRepository,
                        organizationRepository,
                        tokenService);
        offering =
                CourseOffering.builder()
                        .id(OFFERING_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(ORG_ID)
                        .trainingCourseTypeId(55L)
                        .status(CourseOfferingStatus.PUBLISHED)
                        .maxParticipants(5)
                        .firstSessionStart(Instant.parse("2027-03-01T08:00:00Z"))
                        .lastSessionEnd(Instant.parse("2027-03-02T16:00:00Z"))
                        .build();
    }

    @Test
    void summaryCountsSeatsPerStatus() {
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        when(registrationRepository.countByStatus(OFFERING_ID))
                .thenReturn(
                        List.of(
                                count(CourseRegistrationStatus.PARTICIPANT, 3),
                                count(CourseRegistrationStatus.WAITLIST, 2),
                                count(CourseRegistrationStatus.PENDING, 1),
                                count(CourseRegistrationStatus.REJECTED, 4)));

        CourseRegistrationSummaryDto summary = service.summary(ORG_ID, OFFERING_ID);

        assertThat(summary.getMaxParticipants()).isEqualTo(5);
        assertThat(summary.getParticipants()).isEqualTo(3);
        assertThat(summary.getWaitlist()).isEqualTo(2);
        assertThat(summary.getPending()).isEqualTo(1);
        assertThat(summary.getFreeSeats()).isEqualTo(2);
    }

    @Test
    void summaryListsUsageOfEveryQuota() {
        offering.setMaxParticipants(5);
        offering.getAudiences().add(audience(21L, CourseAudienceKind.ORG_SUBTREE, 20L, 2));
        offering.getAudiences().add(audience(20L, CourseAudienceKind.ORG, 10L, 3));
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        when(registrationRepository.countByStatus(OFFERING_ID))
                .thenReturn(
                        List.of(
                                count(CourseRegistrationStatus.PARTICIPANT, 4),
                                count(CourseRegistrationStatus.WAITLIST, 1)));
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(
                        List.of(
                                seats(20L, CourseRegistrationStatus.PARTICIPANT, 3),
                                seats(21L, CourseRegistrationStatus.PARTICIPANT, 1),
                                seats(21L, CourseRegistrationStatus.WAITLIST, 1)));

        CourseRegistrationSummaryDto summary = service.summary(ORG_ID, OFFERING_ID);

        assertThat(summary.getQuotas())
                .extracting(
                        CourseRegistrationSummaryDto.Quota::getAudienceId,
                        CourseRegistrationSummaryDto.Quota::getQuota,
                        CourseRegistrationSummaryDto.Quota::getParticipants,
                        CourseRegistrationSummaryDto.Quota::getWaitlist,
                        CourseRegistrationSummaryDto.Quota::getFreeSeats)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(20L, 3, 3L, 0L, 0L),
                        org.assertj.core.groups.Tuple.tuple(21L, 2, 1L, 1L, 1L));
    }

    @Test
    void summaryWithoutQuotasHasNoQuotaUsage() {
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        when(registrationRepository.countByStatus(OFFERING_ID)).thenReturn(List.of());

        assertThat(service.summary(ORG_ID, OFFERING_ID).getQuotas()).isEmpty();
    }

    @Test
    void waitlistPositionsAreCountedPerAudienceEntry() {
        var positions =
                CourseRegistrationAssembler.waitlistPositions(
                        List.of(
                                waitlistEntry(1L, 20L),
                                waitlistEntry(2L, 21L),
                                waitlistEntry(3L, 20L),
                                waitlistEntry(4L, 21L)));

        assertThat(positions).containsEntry(1L, 1).containsEntry(3L, 2);
        assertThat(positions).containsEntry(2L, 1).containsEntry(4L, 2);
    }

    private static CourseOfferingAudience audience(
            Long id, CourseAudienceKind kind, Long refId, Integer quota) {
        return CourseOfferingAudience.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .kind(kind)
                .refId(refId)
                .quota(quota)
                .build();
    }

    @Test
    void summaryRejectsOfferingOfAnotherOrganization() {
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID + 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summary(ORG_ID + 1, OFFERING_ID))
                .isInstanceOf(CourseOfferingNotFoundException.class);
    }

    @Test
    void listForOfferingNumbersWaitlistByApprovalOrderAndNamesUsers() {
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        CourseRegistration first = registration(1L, 31L, CourseRegistrationStatus.WAITLIST);
        CourseRegistration second = registration(2L, 32L, CourseRegistrationStatus.WAITLIST);
        CourseRegistration participant =
                registration(3L, 33L, CourseRegistrationStatus.PARTICIPANT);
        when(registrationRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(second, first, participant)));
        when(registrationRepository.findWaitlistInOrder(OFFERING_ID))
                .thenReturn(List.of(waitlistEntry(1L, null), waitlistEntry(2L, null)));
        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(
                                user(31L, "Ada", "Lovelace"),
                                user(32L, null, null),
                                user(33L, "Max", "Mustermann")));

        CourseRegistrationPageDto page =
                service.listForOffering(
                        ORG_ID,
                        OFFERING_ID,
                        new ListQuery(null, null, null, null),
                        new CourseRegistrationListFilter(List.of()));

        assertThat(page.getContent())
                .extracting(
                        CourseRegistrationDto::getId,
                        CourseRegistrationDto::getWaitlistPosition,
                        CourseRegistrationDto::getUserDisplayName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, 2, "u32"),
                        org.assertj.core.groups.Tuple.tuple(1L, 1, "Ada Lovelace"),
                        org.assertj.core.groups.Tuple.tuple(3L, null, "Max Mustermann"));
    }

    @Test
    void getForOfferingReturnsRegistrationOfThatOffering() {
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        CourseRegistration pending = registration(4L, 31L, CourseRegistrationStatus.PENDING);
        pending.setWorkflowId(77L);
        when(registrationRepository.findByIdAndOffering_Id(4L, OFFERING_ID))
                .thenReturn(Optional.of(pending));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(31L, "Ada", "Lovelace")));

        CourseRegistrationDto dto = service.getForOffering(ORG_ID, OFFERING_ID, 4L);

        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getUserDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(dto.getWorkflowId()).isEqualTo(77L);
        assertThat(dto.getLinkType()).isEqualTo(AppLinkType.WORKFLOW);
        assertThat(dto.getLinkId()).isEqualTo(77L);
        assertThat(dto.getNavigationPath()).isEqualTo("/my-area/workflows/77");
    }

    @Test
    void getForOfferingRejectsForeignRegistration() {
        when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        when(registrationRepository.findByIdAndOffering_Id(99L, OFFERING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForOffering(ORG_ID, OFFERING_ID, 99L))
                .isInstanceOf(CourseRegistrationNotFoundException.class);
    }

    @Test
    void listForUserShowsStatusInformationOfForeignOfferings() {
        CourseRegistration cancelled = registration(5L, 31L, CourseRegistrationStatus.CANCELLED);
        cancelled.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
        cancelled.setEndReason("internal reason");
        when(registrationRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cancelled)));
        when(offeringRepository.findByIdIn(List.of(OFFERING_ID))).thenReturn(List.of(offering));
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(55L);
        type.setName("Atemschutzgeräteträger");
        when(trainingCourseTypeRepository.findAllById(any())).thenReturn(List.of(type));
        Organization org = new Organization();
        org.setId(ORG_ID);
        org.setName("Feuerwehr A");
        when(organizationRepository.findAllById(any())).thenReturn(List.of(org));

        UserCourseRegistrationPageDto page =
                service.listForUser(
                        31L,
                        new ListQuery(null, null, null, null),
                        new UserCourseRegistrationListFilter(List.of(), false));

        assertThat(page.getContent()).hasSize(1);
        UserCourseRegistrationDto dto = page.getContent().getFirst();
        assertThat(dto.getTrainingCourseTypeName()).isEqualTo("Atemschutzgeräteträger");
        assertThat(dto.getOrganizationName()).isEqualTo("Feuerwehr A");
        assertThat(dto.getFirstSessionStart()).isEqualTo(Instant.parse("2027-03-01T08:00:00Z"));
        assertThat(dto.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
        assertThat(dto.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.ORGANIZATION);
        assertThat(UserCourseRegistrationDto.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("endReason", "userId", "workflowId");
    }

    @Test
    void listForSelfShowsWhatTheCallerMayStillDo() {
        offering.setFirstSessionStart(Instant.now().plusSeconds(86_400));
        CourseRegistration participant =
                registration(1L, 31L, CourseRegistrationStatus.PARTICIPANT);
        CourseRegistration cancelling = registration(2L, 31L, CourseRegistrationStatus.PARTICIPANT);
        cancelling.setCancellationWorkflowId(901L);
        CourseRegistration waitlist = registration(3L, 31L, CourseRegistrationStatus.WAITLIST);
        CourseRegistration removed = registration(4L, 31L, CourseRegistrationStatus.CANCELLED);
        removed.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
        removed.setEndReason("Kein Bedarf");
        CourseRegistration pending = registration(5L, 31L, CourseRegistrationStatus.PENDING);
        when(registrationRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(participant, cancelling, waitlist, removed, pending)));
        when(registrationRepository.findWaitlistInOrder(OFFERING_ID))
                .thenReturn(List.of(waitlistEntry(9L, null), waitlistEntry(3L, null)));
        when(offeringRepository.findByIdIn(List.of(OFFERING_ID))).thenReturn(List.of(offering));
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(55L);
        type.setName("Atemschutzgeräteträger");
        when(trainingCourseTypeRepository.findAllById(any())).thenReturn(List.of(type));
        Organization provider = new Organization();
        provider.setId(ORG_ID);
        provider.setName("Feuerwehr A");
        when(organizationRepository.findAllById(any())).thenReturn(List.of(provider));

        SelfCourseRegistrationPageDto page =
                service.listForSelf(31L, new ListQuery(null, null, null, null));

        assertThat(page.getContent())
                .extracting(
                        SelfCourseRegistrationDto::getId,
                        SelfCourseRegistrationDto::isCanCancel,
                        SelfCourseRegistrationDto::isCancellationPending,
                        SelfCourseRegistrationDto::isCanWithdraw,
                        SelfCourseRegistrationDto::getWaitlistPosition)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, true, false, false, null),
                        org.assertj.core.groups.Tuple.tuple(2L, false, true, false, null),
                        org.assertj.core.groups.Tuple.tuple(3L, false, false, true, 2),
                        org.assertj.core.groups.Tuple.tuple(4L, false, false, false, null),
                        org.assertj.core.groups.Tuple.tuple(5L, false, false, false, null));
        SelfCourseRegistrationDto removedDto = page.getContent().get(3);
        assertThat(removedDto.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.ORGANIZATION);
        assertThat(removedDto.getEndReason()).isEqualTo("Kein Bedarf");
        assertThat(removedDto.getTrainingCourseTypeName()).isEqualTo("Atemschutzgeräteträger");
        assertThat(removedDto.getOrganizationName()).isEqualTo("Feuerwehr A");
    }

    @Test
    void listForSelfCannotCancelOnceTheFirstSessionStarted() {
        offering.setFirstSessionStart(Instant.now().minusSeconds(60));
        when(registrationRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        registration(
                                                1L, 31L, CourseRegistrationStatus.PARTICIPANT))));
        when(offeringRepository.findByIdIn(List.of(OFFERING_ID))).thenReturn(List.of(offering));

        SelfCourseRegistrationPageDto page =
                service.listForSelf(31L, new ListQuery(null, null, null, null));

        assertThat(page.getContent().getFirst().isCanCancel()).isFalse();
    }

    private CourseRegistration registration(Long id, Long userId, CourseRegistrationStatus status) {
        return CourseRegistration.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .offering(offering)
                .userId(userId)
                .status(status)
                .build();
    }

    private static User user(Long id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername("u" + id);
        return user;
    }

    private static CourseRegistrationRepository.WaitlistEntry waitlistEntry(
            Long id, Long audienceId) {
        return new CourseRegistrationRepository.WaitlistEntry() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public Long getAudienceId() {
                return audienceId;
            }
        };
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

    private static CourseRegistrationRepository.StatusCount count(
            CourseRegistrationStatus status, long count) {
        return new CourseRegistrationRepository.StatusCount() {
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

    @SuppressWarnings("unchecked")
    private static Specification<CourseRegistration> anySpec() {
        return any(Specification.class);
    }
}
