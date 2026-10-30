/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseregistrationcancellation;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.courseoffering.service.CourseRegistrationService.CancellationCandidate;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cancellation of a participant place. Both the offering organization and the user's main
 * organization always decide (a single decision if both are the same organization, because approver
 * slots are keyed per organization). Only an approval by all deciders ends the registration, and it
 * does so under the offering lock in {@link CourseRegistrationService#approveCancellation};
 * rejection or abort leave it unchanged.
 */
@Slf4j
@Component
public class CourseRegistrationCancellationWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<
                CourseRegistrationCancellationWorkflowPayload> {

    /** {@code wf_instances.subject_entity_type}; the subject id is the registration id. */
    public static final String SUBJECT_ENTITY_TYPE = "COURSE_REGISTRATION";

    private final CourseRegistrationService registrationService;
    private final OrganizationRepository organizationRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    public CourseRegistrationCancellationWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            CourseRegistrationService registrationService,
            OrganizationRepository organizationRepository,
            TenantTrainingCourseTypeRepository trainingCourseTypeRepository,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.registrationService = registrationService;
        this.organizationRepository = organizationRepository;
        this.trainingCourseTypeRepository = trainingCourseTypeRepository;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.COURSE_REGISTRATION_CANCELLATION;
    }

    @Override
    public WorkflowMode mode() {
        return WorkflowMode.DYNAMIC;
    }

    @Override
    public Class<CourseRegistrationCancellationWorkflowPayload> payloadType() {
        return CourseRegistrationCancellationWorkflowPayload.class;
    }

    /** One user may cancel several registrations at the same time. */
    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    /** The main organization is checked in {@link #validateOpen}, with a message of its own. */
    @Override
    public boolean requiresOpenerOrganizationMembership() {
        return false;
    }

    /** Opened from "my registrations", not from the generic workflow catalog. */
    @Override
    public boolean allowsInputChangeRequests() {
        return false;
    }

    @Override
    public boolean allowsOpenerFreeInputEdit() {
        return false;
    }

    @Override
    public boolean allowsDocumentPromotion(Workflow workflow) {
        return false;
    }

    @Override
    public List<String> resolveOpenConcurrencyKeys(
            WorkflowOpenContext<CourseRegistrationCancellationWorkflowPayload> ctx,
            Workflow existing) {
        CourseRegistrationCancellationWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getRegistrationId() == null) {
            return List.of();
        }
        return List.of(WorkflowOpenConcurrencyKeys.courseRegistration(payload.getRegistrationId()));
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open cancellation for this course registration";
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - course cancellation is not possible";
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<CourseRegistrationCancellationWorkflowPayload> ctx) {
        CourseRegistrationCancellationWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getOfferingOrganizationId() == null) {
            throw new WorkflowValidationException(
                    "The offering organization is only known for a concrete cancellation");
        }
        return payload.getOfferingOrganizationId();
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        throw new WorkflowValidationException(
                "The context organization of a course cancellation depends on the offering");
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        CourseRegistrationCancellationWorkflowPayload payload =
                readPayload(workflow, CourseRegistrationCancellationWorkflowPayload.class);
        return resolveContextOrganization(
                new WorkflowOpenContext<>(
                        workflow.getOpenedByUserId(), workflow.getTenantId(), payload));
    }

    @Override
    public List<ApproverOrg> resolveDynamicOrganizations(
            WorkflowOpenContext<CourseRegistrationCancellationWorkflowPayload> ctx) {
        CourseRegistrationCancellationWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getHomeOrganizationId() == null) {
            return List.of();
        }
        return List.of(new ApproverOrg(payload.getHomeOrganizationId()));
    }

    @Override
    public void validateOpen(
            WorkflowOpenContext<CourseRegistrationCancellationWorkflowPayload> ctx) {
        CourseRegistrationCancellationWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        if (payload.getRegistrationId() == null) {
            throw new WorkflowValidationException("Course registration is required");
        }

        CancellationCandidate candidate =
                registrationService.requireCancellable(
                        ctx.getOpenedByUserId(), payload.getRegistrationId());
        CourseOffering offering = candidate.offering();

        Long homeOrganizationId =
                findMainOrganization(ctx.getOpenedByUserId())
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "A main organization is required: it decides on"
                                                        + " the cancellation"));

        payload.setOfferingId(offering.getId());
        payload.setTrainingCourseTypeName(
                trainingCourseTypeRepository
                        .findById(offering.getTrainingCourseTypeId())
                        .map(TenantTrainingCourseType::getName)
                        .orElse(null));
        payload.setFirstSessionStart(offering.getFirstSessionStart());
        payload.setOfferingOrganizationId(offering.getOrganizationId());
        payload.setOfferingOrganizationName(organizationName(offering.getOrganizationId()));
        payload.setHomeOrganizationId(homeOrganizationId);
        payload.setHomeOrganizationName(organizationName(homeOrganizationId));
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<CourseRegistrationCancellationWorkflowPayload> ctx,
            Workflow existing) {
        throw new WorkflowValidationException(
                "A cancellation cannot be changed; withdraw it and cancel again");
    }

    @Override
    public void onOpened(Workflow workflow) {
        CourseRegistrationCancellationWorkflowPayload payload =
                readPayload(workflow, CourseRegistrationCancellationWorkflowPayload.class);
        registrationService.cancellationOpened(
                workflow.getOpenedByUserId(), payload.getRegistrationId(), workflow.getId());
    }

    @Override
    public void onApproved(Workflow workflow) {
        registrationService.approveCancellation(workflow.getId());
    }

    @Override
    public void onRejected(Workflow workflow) {
        registrationService.cancellationDeclined(workflow.getId());
    }

    @Override
    public void onCancelled(Workflow workflow) {
        registrationService.cancellationDeclined(workflow.getId());
    }

    private String organizationName(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .map(Organization::getName)
                .orElse(null);
    }
}
