/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseregistrationcancellation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.courseoffering.service.CourseRegistrationService.CancellationCandidate;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationCancellationWorkflowHandlerTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 3L;
    private static final long OFFERING_ID = 100L;
    private static final long REGISTRATION_ID = 40L;
    private static final long PROVIDER_ORG_ID = 10L;
    private static final long HOME_ORG_ID = 20L;

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock LocalizedWorkflowDocumentPromotionSupport promotionSupport;
    @Mock CourseRegistrationService registrationService;
    @Mock OrganizationRepository organizationRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    private CourseRegistrationCancellationWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new CourseRegistrationCancellationWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        promotionSupport,
                        registrationService,
                        organizationRepository,
                        trainingCourseTypeRepository,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesDynamicSelfServiceMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.COURSE_REGISTRATION_CANCELLATION);
        assertThat(WorkflowType.COURSE_REGISTRATION_CANCELLATION.getId()).isEqualTo(18L);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.DYNAMIC);
        assertThat(handler.payloadType())
                .isEqualTo(CourseRegistrationCancellationWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
        assertThat(handler.listedInBeginnableCatalog()).isFalse();
        assertThat(handler.allowsInputChangeRequests()).isFalse();
        assertThat(handler.allowsOpenerFreeInputEdit()).isFalse();
        assertThat(handler.requiredPermissionToOpen()).isEmpty();
    }

    @Test
    void offeringOrganizationIsContextApproverAndMainOrganizationAlwaysJoins() {
        CourseRegistrationCancellationWorkflowPayload payload = snapshot(HOME_ORG_ID);

        assertThat(handler.resolveContextOrganization(context(payload))).isEqualTo(PROVIDER_ORG_ID);
        assertThat(handler.resolveDynamicOrganizations(context(payload)))
                .containsExactly(new ApproverOrg(HOME_ORG_ID));
    }

    @Test
    void sameOrganizationAsContextAndDynamicApproverIsKeyedOnceByTheEngine() {
        // WorkflowResolverServiceTest#resolveApproverOrganizationIds_collapsesIdenticalContextAnd
        // DynamicOrganization covers the collapse; here both inputs are the same organization.
        CourseRegistrationCancellationWorkflowPayload payload = snapshot(PROVIDER_ORG_ID);

        assertThat(handler.resolveContextOrganization(context(payload))).isEqualTo(PROVIDER_ORG_ID);
        assertThat(handler.resolveDynamicOrganizations(context(payload)))
                .containsExactly(new ApproverOrg(PROVIDER_ORG_ID));
    }

    @Test
    void contextOrganizationFailsWithoutSnapshot() {
        assertThatThrownBy(
                        () ->
                                handler.resolveContextOrganization(
                                        context(
                                                CourseRegistrationCancellationWorkflowPayload
                                                        .builder()
                                                        .registrationId(REGISTRATION_ID)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Test
    void resolveInstanceOrganizationReadsStoredPayload() {
        Workflow workflow =
                Workflow.builder()
                        .id(9L)
                        .tenantId(TENANT_ID)
                        .openedByUserId(USER_ID)
                        .payload("{\"registrationId\":40,\"offeringOrganizationId\":10}")
                        .build();

        assertThat(handler.resolveInstanceOrganization(workflow)).isEqualTo(PROVIDER_ORG_ID);
    }

    @Test
    void concurrencyKeyIsPerRegistration() {
        assertThat(handler.resolveOpenConcurrencyKeys(context(snapshot(HOME_ORG_ID)), null))
                .containsExactly(WorkflowOpenConcurrencyKeys.courseRegistration(REGISTRATION_ID))
                .containsExactly("course-registration:40");
    }

    @Test
    void validateOpenWritesSnapshotWithBothOrganizations() {
        stubCancellable();
        mainOrganization(HOME_ORG_ID, "Heimat");
        CourseRegistrationCancellationWorkflowPayload payload =
                CourseRegistrationCancellationWorkflowPayload.builder()
                        .registrationId(REGISTRATION_ID)
                        .build();

        handler.validateOpen(context(payload));

        assertThat(payload.getOfferingId()).isEqualTo(OFFERING_ID);
        assertThat(payload.getTrainingCourseTypeName()).isEqualTo("Atemschutzgeräteträger");
        assertThat(payload.getTrainingCourseTypeName()).isEqualTo("Atemschutzgeräteträger");
        assertThat(payload.getFirstSessionStart()).isEqualTo(Instant.parse("2027-03-01T08:00:00Z"));
        assertThat(payload.getOfferingOrganizationId()).isEqualTo(PROVIDER_ORG_ID);
        assertThat(payload.getOfferingOrganizationName()).isEqualTo("Anbieter");
        assertThat(payload.getHomeOrganizationId()).isEqualTo(HOME_ORG_ID);
        assertThat(payload.getHomeOrganizationName()).isEqualTo("Heimat");
    }

    @Test
    void validateOpenRequiresMainOrganization() {
        stubCancellableRegistration();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        context(
                                                CourseRegistrationCancellationWorkflowPayload
                                                        .builder()
                                                        .registrationId(REGISTRATION_ID)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("main organization");
    }

    @Test
    void validateOpenRequiresRegistration() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        context(
                                                new CourseRegistrationCancellationWorkflowPayload())))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Test
    void validateOpenPropagatesCancellationRuleViolations() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));
        when(registrationService.requireCancellable(USER_ID, REGISTRATION_ID))
                .thenThrow(new CourseOfferingValidationException("The offering has started"));

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        context(
                                                CourseRegistrationCancellationWorkflowPayload
                                                        .builder()
                                                        .registrationId(REGISTRATION_ID)
                                                        .build())))
                .hasMessageContaining("started");
    }

    @Test
    void payloadCannotBeUpdatedAfterOpening() {
        assertThatThrownBy(
                        () ->
                                handler.validateUpdate(
                                        context(snapshot(HOME_ORG_ID)), new Workflow()))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Test
    void lifecycleCallbacksDelegateToRegistrationService() {
        Workflow workflow =
                Workflow.builder()
                        .id(9L)
                        .tenantId(TENANT_ID)
                        .openedByUserId(USER_ID)
                        .payload("{\"registrationId\":40}")
                        .build();

        handler.onOpened(workflow);
        handler.onApproved(workflow);
        handler.onRejected(workflow);
        handler.onCancelled(workflow);

        verify(registrationService).cancellationOpened(USER_ID, REGISTRATION_ID, 9L);
        verify(registrationService).approveCancellation(9L);
        // rejection and abort both leave the registration unchanged
        verify(registrationService, org.mockito.Mockito.times(2)).cancellationDeclined(9L);
    }

    private void stubCancellable() {
        stubCancellableRegistration();
        stubNames();
    }

    private void stubCancellableRegistration() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));
        CourseOffering offering =
                CourseOffering.builder()
                        .id(OFFERING_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(PROVIDER_ORG_ID)
                        .trainingCourseTypeId(55L)
                        .firstSessionStart(Instant.parse("2027-03-01T08:00:00Z"))
                        .build();
        when(registrationService.requireCancellable(USER_ID, REGISTRATION_ID))
                .thenReturn(
                        new CancellationCandidate(
                                CourseRegistration.builder().id(REGISTRATION_ID).build(),
                                offering));
    }

    private void stubNames() {
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(55L);
        type.setName("Atemschutzgeräteträger");
        when(trainingCourseTypeRepository.findById(55L)).thenReturn(Optional.of(type));
        when(organizationRepository.findById(PROVIDER_ORG_ID))
                .thenReturn(
                        Optional.of(
                                Organization.builder()
                                        .id(PROVIDER_ORG_ID)
                                        .name("Anbieter")
                                        .build()));
    }

    private void mainOrganization(long organizationId, String name) {
        Organization organization = Organization.builder().id(organizationId).name(name).build();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    }

    private static CourseRegistrationCancellationWorkflowPayload snapshot(Long homeOrganizationId) {
        return CourseRegistrationCancellationWorkflowPayload.builder()
                .registrationId(REGISTRATION_ID)
                .offeringId(OFFERING_ID)
                .offeringOrganizationId(PROVIDER_ORG_ID)
                .homeOrganizationId(homeOrganizationId)
                .build();
    }

    private static WorkflowOpenContext<CourseRegistrationCancellationWorkflowPayload> context(
            CourseRegistrationCancellationWorkflowPayload payload) {
        return new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload);
    }
}
