/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.dto.OnboardingInvitationPageDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.platform.web.list.ListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/onboarding-invitations")
@RequiredArgsConstructor
public class OnboardingInvitationOrganizationController {

    private final OnboardingInvitationService onboardingInvitationService;
    private final OnboardingInvitationProgressService onboardingInvitationProgressService;

    @GetMapping
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<OnboardingInvitationPageDto> listInvitations(
            @PathVariable Long organizationId, ListQuery query) {
        return ResponseEntity.ok(
                onboardingInvitationService.listForOrganization(organizationId, query));
    }

    @GetMapping("/{invitationId}")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> getInvitation(
            @PathVariable Long organizationId, @PathVariable Long invitationId) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.getDetailForOrganization(
                        organizationId, invitationId));
    }

    @GetMapping("/{invitationId}/documents/{configKey}/template")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadTemplate(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadTemplateForOrganization(
                        organizationId, invitationId, configKey));
    }

    @GetMapping("/{invitationId}/documents/{configKey}/file")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadSignedDocument(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadSignedDocumentForOrganization(
                        organizationId, invitationId, configKey));
    }

    @GetMapping("/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/template")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadOrganizationTemplate(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadOrganizationTemplateForOrganization(
                        organizationId, invitationId, configKey));
    }

    @GetMapping("/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/file")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadOrganizationSignedDocument(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService
                        .downloadOrganizationSignedDocumentForOrganization(
                                organizationId, invitationId, configKey));
    }

    @GetMapping("/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadRecordAttachment(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadRecordAttachmentForOrganization(
                        organizationId, invitationId, stepType, recordKey, attachmentIndex));
    }

    @GetMapping("/{invitationId}/steps/DRIVING_LICENSE/attachment")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadDrivingLicenseAttachment(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadDrivingLicenseAttachmentForOrganization(
                        organizationId, invitationId, attachmentIndex));
    }

    @GetMapping("/{invitationId}/steps/RANK/attachment")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadRankAttachment(
            @PathVariable Long organizationId,
            @PathVariable Long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadRankAttachmentForOrganization(
                        organizationId, invitationId, attachmentIndex));
    }
}
