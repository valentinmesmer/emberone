/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseofferingregistration;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.courseoffering.service.CourseRegistrationService.Candidate;
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
 * Registration for a course offering. The offering organization always decides; when the offering
 * requires it, the user's main organization decides as well (a single decision if both are the same
 * organization, because approver slots are keyed per organization). Seat or waitlist assignment
 * happens in {@link CourseRegistrationService#approve} under a lock on the offering.
 */
@Slf4j
@Component
public class CourseOfferingRegistrationWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<
                CourseOfferingRegistrationWorkflowPayload> {

    /** {@code wf_instances.subject_entity_type}; the subject id is the offering id. */
    public static final String SUBJECT_ENTITY_TYPE = "COURSE_OFFERING";

    private final CourseRegistrationService registrationService;
    private final OrganizationRepository organizationRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    public CourseOfferingRegistrationWorkflowHandler(
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
        return WorkflowType.COURSE_OFFERING_REGISTRATION;
    }

    @Override
    public WorkflowMode mode() {
        return WorkflowMode.DYNAMIC;
    }

    @Override
    public Class<CourseOfferingRegistrationWorkflowPayload> payloadType() {
        return CourseOfferingRegistrationWorkflowPayload.class;
    }

    /** One user may register for several offerings at the same time. */
    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return true;
    }

    /**
     * Any active membership suffices (the engine checks it); the main organization is only needed
     * when the offering requires its approval, which {@link #validateOpen} enforces.
     */
    @Override
    public boolean requiresOpenerOrganizationMembership() {
        return false;
    }

    /** Opened from the offering list, not from the generic workflow catalog. */
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
            WorkflowOpenContext<CourseOfferingRegistrationWorkflowPayload> ctx, Workflow existing) {
        CourseOfferingRegistrationWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getOfferingId() == null) {
            return List.of();
        }
        return List.of(WorkflowOpenConcurrencyKeys.courseOffering(payload.getOfferingId()));
    }

    @Override
    public String openConcurrencyConflictMessage() {
        return "You already have an open registration for this course offering";
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - course registration is not possible";
    }

    @Override
    public Long resolveContextOrganization(
            WorkflowOpenContext<CourseOfferingRegistrationWorkflowPayload> ctx) {
        CourseOfferingRegistrationWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getOfferingOrganizationId() == null) {
            throw new WorkflowValidationException(
                    "The offering organization is only known for a concrete registration");
        }
        return payload.getOfferingOrganizationId();
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        throw new WorkflowValidationException(
                "The context organization of a course registration depends on the offering");
    }

    @Override
    public Long resolveInstanceOrganization(Workflow workflow) {
        CourseOfferingRegistrationWorkflowPayload payload =
                readPayload(workflow, CourseOfferingRegistrationWorkflowPayload.class);
        return resolveContextOrganization(
                new WorkflowOpenContext<>(
                        workflow.getOpenedByUserId(), workflow.getTenantId(), payload));
    }

    @Override
    public List<ApproverOrg> resolveDynamicOrganizations(
            WorkflowOpenContext<CourseOfferingRegistrationWorkflowPayload> ctx) {
        CourseOfferingRegistrationWorkflowPayload payload = ctx.getPayload();
        if (payload == null || payload.getHomeOrganizationId() == null) {
            return List.of();
        }
        return List.of(new ApproverOrg(payload.getHomeOrganizationId()));
    }

    @Override
    public void validateOpen(WorkflowOpenContext<CourseOfferingRegistrationWorkflowPayload> ctx) {
        CourseOfferingRegistrationWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        if (payload.getOfferingId() == null) {
            throw new WorkflowValidationException("Course offering is required");
        }

        Candidate candidate =
                registrationService.requireRegistrable(
                        ctx.getOpenedByUserId(), payload.getOfferingId());
        CourseOffering offering = candidate.offering();

        Long homeOrganizationId = null;
        if (offering.isRequiresHomeOrgApproval()) {
            homeOrganizationId =
                    findMainOrganization(ctx.getOpenedByUserId())
                            .orElseThrow(
                                    () ->
                                            new WorkflowValidationException(
                                                    "A main organization is required: this"
                                                            + " offering needs its approval"));
        }

        payload.setTrainingCourseTypeId(offering.getTrainingCourseTypeId());
        payload.setTrainingCourseTypeName(
                trainingCourseTypeRepository
                        .findById(offering.getTrainingCourseTypeId())
                        .map(TenantTrainingCourseType::getName)
                        .orElse(null));
        payload.setOfferingOrganizationId(offering.getOrganizationId());
        payload.setOfferingOrganizationName(organizationName(offering.getOrganizationId()));
        payload.setHomeOrganizationId(homeOrganizationId);
        payload.setHomeOrganizationName(
                homeOrganizationId == null ? null : organizationName(homeOrganizationId));
        payload.setAlreadyHeld(candidate.alreadyHeld());
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<CourseOfferingRegistrationWorkflowPayload> ctx, Workflow existing) {
        throw new WorkflowValidationException(
                "A registration cannot be changed; cancel it and register again");
    }

    @Override
    public void onOpened(Workflow workflow) {
        CourseOfferingRegistrationWorkflowPayload payload =
                readPayload(workflow, CourseOfferingRegistrationWorkflowPayload.class);
        registrationService.createPending(
                workflow.getTenantId(),
                workflow.getOpenedByUserId(),
                payload.getOfferingId(),
                workflow.getId(),
                Boolean.TRUE.equals(payload.getAlreadyHeld()));
    }

    @Override
    public void onApproved(Workflow workflow) {
        registrationService.approve(workflow.getId());
    }

    @Override
    public void onRejected(Workflow workflow) {
        registrationService.reject(workflow.getId());
    }

    @Override
    public void onCancelled(Workflow workflow) {
        registrationService.cancelByUser(workflow.getId());
    }

    private String organizationName(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .map(Organization::getName)
                .orElse(null);
    }
}
