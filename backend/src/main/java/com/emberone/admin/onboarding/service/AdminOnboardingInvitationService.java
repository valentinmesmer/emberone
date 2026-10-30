/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboarding.service;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.dto.OnboardingStepsAvailableDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationReadinessService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TokenService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminOnboardingInvitationService {

    private final OrganizationRepository organizationRepository;
    private final OnboardingInvitationReadinessService onboardingInvitationReadinessService;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final OnboardingInvitationService onboardingInvitationService;
    private final OnboardingInvitationProgressService progressService;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public OnboardingStepsAvailableDto getOnboardingAvailable(long tenantId, long organizationId) {
        Organization organization = requireOrganizationInTenant(tenantId, organizationId);
        return OnboardingStepsAvailableDto.builder()
                .available(
                        onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(
                                organization))
                .build();
    }

    /**
     * Whether admin recording can be started for this member: active org membership, configured
     * steps, and no invitation yet for the user and organization.
     */
    @Transactional(readOnly = true)
    public OnboardingStepsAvailableDto getRecordingAvailable(
            long tenantId, long organizationId, long userId) {
        Organization organization = requireOrganizationInTenant(tenantId, organizationId);
        if (!membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                userId, organizationId, Instant.now())) {
            return OnboardingStepsAvailableDto.builder().available(false).build();
        }
        if (onboardingInvitationService.hasInvitationForUserAndOrganization(
                userId, organizationId)) {
            return OnboardingStepsAvailableDto.builder().available(false).build();
        }
        return OnboardingStepsAvailableDto.builder()
                .available(
                        onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(
                                organization))
                .build();
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto startRecording(
            long tenantId, long organizationId, long userId) {
        Organization organization = requireOrganizationInTenant(tenantId, organizationId);
        if (!membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                userId, organizationId, Instant.now())) {
            throw new ConflictException(
                    "User must be an active member of the organization to record onboarding");
        }
        if (!onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(organization)) {
            throw new ConflictException("Organization has no onboarding steps configured");
        }
        OnboardingInvitation invitation =
                onboardingInvitationService.createForExistingUser(
                        tenantId, userId, organizationId, tokenService.getReferencedUserId());
        return progressService.getDetailForOrganization(organizationId, invitation.getId());
    }

    @Transactional(readOnly = true)
    public SelfOnboardingInvitationDetailDto getDetail(
            long tenantId, long organizationId, long invitationId) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.getDetailForOrganization(organizationId, invitationId);
    }

    /**
     * Onboarding invitation for a user in this organization (any status), or 404 if none exists.
     */
    @Transactional(readOnly = true)
    public SelfOnboardingInvitationDetailDto getDetailForUser(
            long tenantId, long organizationId, long userId) {
        requireOrganizationInTenant(tenantId, organizationId);
        OnboardingInvitation invitation =
                onboardingInvitationService.requireInvitationForOrganizationUser(
                        organizationId, userId);
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Invitation does not belong to tenant");
        }
        return progressService.getDetailForOrganization(organizationId, invitation.getId());
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto saveStep(
            long tenantId,
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            Map<String, Object> payload) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.saveStepForOrganization(
                organizationId, invitationId, stepType, payload);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadSignedDocument(
            long tenantId,
            long organizationId,
            long invitationId,
            String configKey,
            MultipartFile file) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.uploadSignedDocumentForOrganization(
                organizationId, invitationId, configKey, file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadRecordAttachment(
            long tenantId,
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            MultipartFile file) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.uploadRecordAttachmentForOrganization(
                organizationId, invitationId, stepType, recordKey, file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadDrivingLicenseAttachment(
            long tenantId, long organizationId, long invitationId, MultipartFile file) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.uploadDrivingLicenseAttachmentForOrganization(
                organizationId, invitationId, file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadRankAttachment(
            long tenantId, long organizationId, long invitationId, MultipartFile file) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.uploadRankAttachmentForOrganization(
                organizationId, invitationId, file);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteRecordAttachment(
            long tenantId,
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.deleteRecordAttachmentForOrganization(
                organizationId, invitationId, stepType, recordKey, attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteDrivingLicenseAttachment(
            long tenantId, long organizationId, long invitationId, int attachmentIndex) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.deleteDrivingLicenseAttachmentForOrganization(
                organizationId, invitationId, attachmentIndex);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto deleteRankAttachment(
            long tenantId, long organizationId, long invitationId, int attachmentIndex) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.deleteRankAttachmentForOrganization(
                organizationId, invitationId, attachmentIndex);
    }

    @Transactional
    public OnboardingDocumentTemplateService.TemplateDownload downloadTemplate(
            long tenantId, long organizationId, long invitationId, String configKey) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadTemplateForOrganization(
                organizationId, invitationId, configKey);
    }

    @Transactional
    public OrganizationOnboardingDocumentTemplateService.TemplateDownload
            downloadOrganizationTemplate(
                    long tenantId, long organizationId, long invitationId, String configKey) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadOrganizationTemplateForOrganization(
                organizationId, invitationId, configKey);
    }

    @Transactional(readOnly = true)
    public OnboardingInvitationProgressService.StoredUpload downloadSignedDocument(
            long tenantId, long organizationId, long invitationId, String configKey) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadSignedDocumentForOrganization(
                organizationId, invitationId, configKey);
    }

    @Transactional(readOnly = true)
    public OnboardingInvitationProgressService.StoredUpload downloadOrganizationSignedDocument(
            long tenantId, long organizationId, long invitationId, String configKey) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadOrganizationSignedDocumentForOrganization(
                organizationId, invitationId, configKey);
    }

    @Transactional
    public SelfOnboardingInvitationDetailDto uploadOrganizationSignedDocument(
            long tenantId,
            long organizationId,
            long invitationId,
            String configKey,
            MultipartFile file) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.uploadOrganizationSignedDocumentForOrganization(
                organizationId, invitationId, configKey, file);
    }

    @Transactional(readOnly = true)
    public OnboardingInvitationProgressService.StoredUpload downloadRecordAttachment(
            long tenantId,
            long organizationId,
            long invitationId,
            OnboardingStepType stepType,
            String recordKey,
            int attachmentIndex) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadRecordAttachmentForOrganization(
                organizationId, invitationId, stepType, recordKey, attachmentIndex);
    }

    @Transactional(readOnly = true)
    public OnboardingInvitationProgressService.StoredUpload downloadDrivingLicenseAttachment(
            long tenantId, long organizationId, long invitationId, int attachmentIndex) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadDrivingLicenseAttachmentForOrganization(
                organizationId, invitationId, attachmentIndex);
    }

    @Transactional(readOnly = true)
    public OnboardingInvitationProgressService.StoredUpload downloadRankAttachment(
            long tenantId, long organizationId, long invitationId, int attachmentIndex) {
        requireOrganizationInTenant(tenantId, organizationId);
        return progressService.downloadRankAttachmentForOrganization(
                organizationId, invitationId, attachmentIndex);
    }

    @Transactional
    public void complete(
            long tenantId, long organizationId, long invitationId, String adminRecordReason) {
        requireOrganizationInTenant(tenantId, organizationId);
        OnboardingInvitation invitation =
                onboardingInvitationService.requireInvitationForOrganization(
                        organizationId, invitationId);
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Invitation does not belong to tenant");
        }
        onboardingInvitationService.recordCompletedOnboarding(
                tenantId, invitationId, tokenService.getReferencedUserId(), adminRecordReason);
    }

    private Organization requireOrganizationInTenant(long tenantId, long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: " + organizationId));
        if (!organization.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Organization does not belong to this tenant");
        }
        return organization;
    }
}
