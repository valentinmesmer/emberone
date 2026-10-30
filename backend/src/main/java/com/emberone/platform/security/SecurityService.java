/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.permission.domain.PermissionType;
import com.emberone.workflow.onboardingsubmission.OnboardingSubmissionWorkflowOpenAccess;
import com.emberone.workflow.organizationmembershipend.OrganizationMembershipEndRequestWorkflowOpenAccess;
import com.emberone.workflow.organizationusermainorganizationchange.OrganizationUserMainOrganizationChangeWorkflowOpenAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SpEL / {@code @PreAuthorize} facade for authorization checks. Domain logic lives in focused
 * access services; this class only delegates.
 */
@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final TokenService tokenService;
    private final AdminAccessService adminAccessService;
    private final OrganizationAccessService organizationAccessService;
    private final WorkflowOpenAccess workflowOpenAccess;
    private final WorkflowInstanceAccess workflowInstanceAccess;
    private final OnboardingSubmissionWorkflowOpenAccess onboardingSubmissionWorkflowOpenAccess;
    private final OrganizationMembershipEndRequestWorkflowOpenAccess
            organizationMembershipEndRequestWorkflowOpenAccess;
    private final OrganizationUserMainOrganizationChangeWorkflowOpenAccess
            organizationUserMainOrganizationChangeWorkflowOpenAccess;

    // --- Admin ---

    public boolean isGlobalAdmin() {
        return adminAccessService.isGlobalAdmin();
    }

    public boolean isTenantAdmin() {
        return adminAccessService.isTenantAdmin();
    }

    public boolean canListTenants() {
        return adminAccessService.canListTenants();
    }

    public boolean canAccessTenant(long tenantId) {
        return adminAccessService.canAccessTenant(tenantId);
    }

    public boolean isAdminUser() {
        return adminAccessService.isAdminUser();
    }

    // --- Organization / permission ---

    public boolean isSelfServiceUser() {
        return adminAccessService.isSelfServiceUser();
    }

    public boolean isActiveOrganizationMember(Long organizationId) {
        return organizationAccessService.isActiveOrganizationMember(organizationId);
    }

    public boolean hasOrganizationGroupReadPermissionForOrganization(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.ORGANIZATION_GROUP_READ);
    }

    public boolean hasOrganizationGroupReadPermission(Long organizationGroupId) {
        return organizationAccessService.hasOrganizationGroupReadPermission(organizationGroupId);
    }

    public boolean hasOrganizationPermission(Long organizationId, PermissionType permissionType) {
        return organizationAccessService.hasOrganizationPermission(organizationId, permissionType);
    }

    public boolean hasUserReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.USER_READ);
    }

    public boolean hasProfileImageReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.PROFILE_IMAGE_READ);
    }

    public boolean hasUserWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.USER_WRITE);
    }

    public boolean hasUserWritePermissionForMembership(Long membershipId) {
        return organizationAccessService.hasUserWritePermissionForMembership(membershipId);
    }

    public boolean hasUserWritePermissionForOrganizationMembership(Long membershipId) {
        return organizationAccessService.hasUserWritePermissionForOrganizationMembership(
                membershipId);
    }

    public boolean hasUserWritePermissionForOrganizationGroup(Long organizationGroupId) {
        return organizationAccessService.hasUserWritePermissionForOrganizationGroup(
                organizationGroupId);
    }

    // --- Events ---

    public boolean hasServiceEventReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.SERVICE_EVENT_READ);
    }

    public boolean hasServiceEventWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.SERVICE_EVENT_WRITE);
    }

    public boolean hasOperationEventReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.OPERATION_EVENT_READ);
    }

    public boolean hasOperationEventWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.OPERATION_EVENT_WRITE);
    }

    public boolean hasEventAttendanceReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.EVENT_ATTENDANCE_READ);
    }

    public boolean hasEventAttendanceWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.EVENT_ATTENDANCE_WRITE);
    }

    public boolean hasEventCategoryReadPermission(Long organizationId, String eventCategory) {
        return organizationAccessService.hasEventCategoryReadPermission(
                organizationId, eventCategory);
    }

    // --- Appointments ---

    public boolean hasAppointmentReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.APPOINTMENT_READ);
    }

    public boolean hasAppointmentWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.APPOINTMENT_WRITE);
    }

    // --- Course offerings ---

    public boolean hasCourseOfferingReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.COURSE_OFFERING_READ);
    }

    public boolean hasCourseOfferingWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.COURSE_OFFERING_WRITE);
    }

    // --- Emergency contacts / absences ---

    public boolean hasEmergencyContactReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.EMERGENCY_CONTACT_READ);
    }

    public boolean hasEmergencyContactWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.EMERGENCY_CONTACT_WRITE);
    }

    public boolean hasAbsenceReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.ABSENCE_READ);
    }

    public boolean hasAbsenceWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.ABSENCE_WRITE);
    }

    public boolean hasTimeEntryReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.TIME_ENTRY_READ);
    }

    public boolean hasTimeEntryWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.TIME_ENTRY_WRITE);
    }

    public boolean hasWorkflowReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.WORKFLOW_READ);
    }

    // --- Documents ---

    public boolean hasDocumentReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.DOCUMENT_READ);
    }

    public boolean hasDocumentWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.DOCUMENT_WRITE);
    }

    // --- Certifications (org-scoped HR/manager API only; self-service uses workflow gates) ---

    public boolean hasCertificationReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.CERTIFICATION_READ);
    }

    public boolean hasCertificationWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.CERTIFICATION_WRITE);
    }

    public boolean hasEventCertificationGrantReadPermission(Long organizationId) {
        return hasEventAttendanceReadPermission(organizationId)
                && hasCertificationReadPermission(organizationId);
    }

    public boolean hasEventCertificationGrantWritePermission(Long organizationId) {
        return hasEventAttendanceReadPermission(organizationId)
                && hasCertificationWritePermission(organizationId);
    }

    public boolean hasTrainingCourseReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.TRAINING_COURSE_READ);
    }

    public boolean hasTrainingCourseWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.TRAINING_COURSE_WRITE);
    }

    public boolean hasDrivingLicenseReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.DRIVING_LICENSE_READ);
    }

    public boolean hasDrivingLicenseWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.DRIVING_LICENSE_WRITE);
    }

    public boolean hasQualificationReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.QUALIFICATION_READ);
    }

    public boolean hasQualificationWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.QUALIFICATION_WRITE);
    }

    public boolean hasRankReadPermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.RANK_READ);
    }

    public boolean hasRankWritePermission(Long organizationId) {
        return hasOrganizationPermission(organizationId, PermissionType.RANK_WRITE);
    }

    public boolean canDownloadWorkflowAttachment(Long workflowId) {
        return canView(workflowId);
    }

    public boolean canUploadWorkflowDraftAttachment(long workflowTypeId) {
        return canBeginSelfServiceWorkflow(workflowTypeId);
    }

    // --- Workflows ---

    public boolean canView(Long workflowId) {
        return workflowInstanceAccess.canView(workflowId, currentUserId());
    }

    public boolean canDecide(Long workflowId, Long participantId) {
        return workflowInstanceAccess.canDecide(workflowId, participantId, currentUserId());
    }

    public boolean canClaim(Long workflowId, Long participantId) {
        return workflowInstanceAccess.canClaim(workflowId, participantId, currentUserId());
    }

    public boolean canUnclaim(Long workflowId, Long participantId) {
        return workflowInstanceAccess.canUnclaim(workflowId, participantId, currentUserId());
    }

    public boolean canWithdrawApproval(Long workflowId, Long participantId) {
        return workflowInstanceAccess.canWithdrawApproval(
                workflowId, participantId, currentUserId());
    }

    public boolean canCancel(Long workflowId) {
        return workflowInstanceAccess.canCancel(workflowId, currentUserId());
    }

    public boolean canUpdatePayload(Long workflowId) {
        return workflowInstanceAccess.canUpdatePayload(workflowId, currentUserId());
    }

    public boolean canRequestInputChange(Long workflowId) {
        return workflowInstanceAccess.canRequestInputChange(workflowId, currentUserId());
    }

    public boolean canWithdrawInputChange(Long workflowId) {
        return workflowInstanceAccess.canWithdrawInputChange(workflowId, currentUserId());
    }

    public boolean canComment(Long workflowId) {
        return workflowInstanceAccess.canComment(workflowId, currentUserId());
    }

    public boolean canUpdateDocumentPromotions(Long workflowId) {
        return workflowInstanceAccess.canUpdateDocumentPromotions(workflowId, currentUserId());
    }

    public boolean canBeginSelfServiceWorkflow(long workflowTypeId) {
        return workflowOpenAccess.canBeginSelfServiceWorkflow(workflowTypeId);
    }

    public boolean canSubmitOnboardingInvitation(long invitationId) {
        return onboardingSubmissionWorkflowOpenAccess.canSubmit(invitationId);
    }

    public boolean canOpenOrganizationMembershipEndWorkflow(
            long organizationId, long targetUserId) {
        return organizationMembershipEndRequestWorkflowOpenAccess.canOpen(
                organizationId, targetUserId);
    }

    public boolean canOpenOrganizationUserMainOrganizationChangeWorkflow(
            long organizationId, long targetUserId) {
        return organizationUserMainOrganizationChangeWorkflowOpenAccess.canOpen(
                organizationId, targetUserId);
    }

    private Long currentUserId() {
        return tokenService.getUserId();
    }
}
