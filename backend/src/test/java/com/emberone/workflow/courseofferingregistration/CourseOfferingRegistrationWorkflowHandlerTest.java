/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseofferingregistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.courseoffering.service.CourseRegistrationService.Candidate;
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
class CourseOfferingRegistrationWorkflowHandlerTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 3L;
    private static final long OFFERING_ID = 100L;
    private static final long PROVIDER_ORG_ID = 10L;
    private static final long HOME_ORG_ID = 20L;

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock LocalizedWorkflowDocumentPromotionSupport promotionSupport;
    @Mock CourseRegistrationService registrationService;
    @Mock OrganizationRepository organizationRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    private CourseOfferingRegistrationWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new CourseOfferingRegistrationWorkflowHandler(
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
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.COURSE_OFFERING_REGISTRATION);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.DYNAMIC);
        assertThat(handler.payloadType())
                .isEqualTo(CourseOfferingRegistrationWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
        assertThat(handler.mayOpenWithoutActiveOrganizationMembership()).isFalse();
        assertThat(handler.listedInBeginnableCatalog()).isFalse();
        assertThat(handler.allowsInputChangeRequests()).isFalse();
        assertThat(handler.allowsOpenerFreeInputEdit()).isFalse();
        assertThat(handler.requiredPermissionToOpen()).isEmpty();
    }

    @Test
    void offeringOrganizationIsAlwaysTheContextApprover() {
        CourseOfferingRegistrationWorkflowPayload payload = snapshot(HOME_ORG_ID);

        assertThat(handler.resolveContextOrganization(context(payload))).isEqualTo(PROVIDER_ORG_ID);
    }

    @Test
    void homeOrganizationIsDynamicApproverOnlyWhenRequired() {
        assertThat(handler.resolveDynamicOrganizations(context(snapshot(HOME_ORG_ID))))
                .containsExactly(new ApproverOrg(HOME_ORG_ID));
        assertThat(handler.resolveDynamicOrganizations(context(snapshot(null)))).isEmpty();
    }

    @Test
    void contextOrganizationFailsWithoutSnapshot() {
        assertThatThrownBy(
                        () ->
                                handler.resolveContextOrganization(
                                        context(
                                                CourseOfferingRegistrationWorkflowPayload.builder()
                                                        .offeringId(OFFERING_ID)
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
                        .payload("{\"offeringId\":100,\"offeringOrganizationId\":10}")
                        .build();

        assertThat(handler.resolveInstanceOrganization(workflow)).isEqualTo(PROVIDER_ORG_ID);
    }

    @Test
    void concurrencyKeyIsPerOffering() {
        assertThat(handler.resolveOpenConcurrencyKeys(context(snapshot(null)), null))
                .containsExactly(WorkflowOpenConcurrencyKeys.courseOffering(OFFERING_ID))
                .containsExactly("course-offering:100");
    }

    @Test
    void validateOpenWritesSnapshotWithoutHomeOrganizationByDefault() {
        stubValidRegistration(offering(false), true);
        CourseOfferingRegistrationWorkflowPayload payload =
                CourseOfferingRegistrationWorkflowPayload.builder().offeringId(OFFERING_ID).build();

        handler.validateOpen(context(payload));

        assertThat(payload.getTrainingCourseTypeId()).isEqualTo(55L);
        assertThat(payload.getTrainingCourseTypeName()).isEqualTo("Atemschutzgeräteträger");
        assertThat(payload.getOfferingOrganizationId()).isEqualTo(PROVIDER_ORG_ID);
        assertThat(payload.getOfferingOrganizationName()).isEqualTo("Anbieter");
        assertThat(payload.getHomeOrganizationId()).isNull();
        assertThat(payload.getHomeOrganizationName()).isNull();
        assertThat(payload.getAlreadyHeld()).isTrue();
    }

    @Test
    void validateOpenAddsMainOrganizationWhenOfferingRequiresHomeApproval() {
        stubValidRegistration(offering(true), false);
        mainOrganization(HOME_ORG_ID, "Heimat");
        CourseOfferingRegistrationWorkflowPayload payload =
                CourseOfferingRegistrationWorkflowPayload.builder().offeringId(OFFERING_ID).build();

        handler.validateOpen(context(payload));

        assertThat(payload.getHomeOrganizationId()).isEqualTo(HOME_ORG_ID);
        assertThat(payload.getHomeOrganizationName()).isEqualTo("Heimat");
        assertThat(payload.getAlreadyHeld()).isFalse();
        assertThat(handler.resolveDynamicOrganizations(context(payload)))
                .containsExactly(new ApproverOrg(HOME_ORG_ID));
    }

    @Test
    void validateOpenRequiresMainOrganizationWhenHomeApprovalIsRequired() {
        stubRegistrable(offering(true), false);
        when(membershipRepository.findActiveByUser_IdAndMainTrue(
                        org.mockito.ArgumentMatchers.eq(USER_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        context(
                                                CourseOfferingRegistrationWorkflowPayload.builder()
                                                        .offeringId(OFFERING_ID)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("main organization");
    }

    @Test
    void validateOpenRequiresOffering() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        context(new CourseOfferingRegistrationWorkflowPayload())))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Test
    void validateOpenPropagatesRegistrationRuleViolations() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));
        when(registrationService.requireRegistrable(USER_ID, OFFERING_ID))
                .thenThrow(
                        new com.emberone.courseoffering.exception.CourseOfferingValidationException(
                                "Registration is not open"));

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        context(
                                                CourseOfferingRegistrationWorkflowPayload.builder()
                                                        .offeringId(OFFERING_ID)
                                                        .build())))
                .hasMessageContaining("not open");
    }

    @Test
    void payloadCannotBeUpdatedAfterOpening() {
        assertThatThrownBy(() -> handler.validateUpdate(context(snapshot(null)), new Workflow()))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Test
    void lifecycleCallbacksDelegateToRegistrationService() {
        Workflow workflow =
                Workflow.builder()
                        .id(9L)
                        .tenantId(TENANT_ID)
                        .openedByUserId(USER_ID)
                        .payload("{\"offeringId\":100,\"alreadyHeld\":true}")
                        .build();

        handler.onOpened(workflow);
        handler.onApproved(workflow);
        handler.onRejected(workflow);
        handler.onCancelled(workflow);

        verify(registrationService).createPending(TENANT_ID, USER_ID, OFFERING_ID, 9L, true);
        verify(registrationService).approve(9L);
        verify(registrationService).reject(9L);
        verify(registrationService).cancelByUser(9L);
    }

    private void stubValidRegistration(CourseOffering offering, boolean alreadyHeld) {
        stubRegistrable(offering, alreadyHeld);
        stubNames();
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

    private void stubRegistrable(CourseOffering offering, boolean alreadyHeld) {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().id(USER_ID).tenantId(TENANT_ID).build()));
        when(registrationService.requireRegistrable(USER_ID, OFFERING_ID))
                .thenReturn(new Candidate(offering, alreadyHeld));
    }

    private void mainOrganization(long organizationId, String name) {
        Organization organization = Organization.builder().id(organizationId).name(name).build();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(
                        org.mockito.ArgumentMatchers.eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
    }

    private static CourseOffering offering(boolean requiresHomeOrgApproval) {
        return CourseOffering.builder()
                .id(OFFERING_ID)
                .tenantId(TENANT_ID)
                .organizationId(PROVIDER_ORG_ID)
                .trainingCourseTypeId(55L)
                .requiresHomeOrgApproval(requiresHomeOrgApproval)
                .build();
    }

    private static CourseOfferingRegistrationWorkflowPayload snapshot(Long homeOrganizationId) {
        return CourseOfferingRegistrationWorkflowPayload.builder()
                .offeringId(OFFERING_ID)
                .offeringOrganizationId(PROVIDER_ORG_ID)
                .homeOrganizationId(homeOrganizationId)
                .build();
    }

    private static WorkflowOpenContext<CourseOfferingRegistrationWorkflowPayload> context(
            CourseOfferingRegistrationWorkflowPayload payload) {
        return new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload);
    }
}
