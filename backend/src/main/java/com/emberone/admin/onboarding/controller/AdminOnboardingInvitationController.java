/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboarding.controller;

import com.emberone.admin.onboarding.dto.CompleteAdminOnboardingRequest;
import com.emberone.admin.onboarding.service.AdminOnboardingInvitationService;
import com.emberone.onboarding.controller.OnboardingInvitationFileResponses;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.dto.OnboardingStepsAvailableDto;
import com.emberone.onboarding.dto.SaveOnboardingStepRequest;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}")
@RequiredArgsConstructor
public class AdminOnboardingInvitationController {

    private final AdminOnboardingInvitationService adminOnboardingInvitationService;

    @GetMapping("/onboarding-available")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OnboardingStepsAvailableDto> getOnboardingAvailable(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.getOnboardingAvailable(tenantId, organizationId));
    }

    @GetMapping("/users/{userId}/onboarding-recording-available")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OnboardingStepsAvailableDto> getRecordingAvailable(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long userId) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.getRecordingAvailable(
                        tenantId, organizationId, userId));
    }

    @PostMapping("/users/{userId}/onboarding-invitations")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> startRecording(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        adminOnboardingInvitationService.startRecording(
                                tenantId, organizationId, userId));
    }

    @GetMapping("/users/{userId}/onboarding-invitation")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> getDetailForUser(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long userId) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.getDetailForUser(
                        tenantId, organizationId, userId));
    }

    @GetMapping("/onboarding-invitations/{invitationId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> getDetail(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.getDetail(tenantId, organizationId, invitationId));
    }

    @PutMapping("/onboarding-invitations/{invitationId}/steps/{stepType}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> saveStep(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @Valid @RequestBody SaveOnboardingStepRequest request) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.saveStep(
                        tenantId, organizationId, invitationId, stepType, request.getPayload()));
    }

    @PostMapping("/onboarding-invitations/{invitationId}/complete")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> complete(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @Valid @RequestBody CompleteAdminOnboardingRequest request) {
        adminOnboardingInvitationService.complete(
                tenantId, organizationId, invitationId, request.getAdminRecordReason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/onboarding-invitations/{invitationId}/documents/{configKey}/template")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadTemplate(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadTemplate(
                        tenantId, organizationId, invitationId, configKey));
    }

    @GetMapping("/onboarding-invitations/{invitationId}/documents/{configKey}/file")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadSignedDocument(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadSignedDocument(
                        tenantId, organizationId, invitationId, configKey));
    }

    @PostMapping("/onboarding-invitations/{invitationId}/documents/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadSignedDocument(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.uploadSignedDocument(
                        tenantId, organizationId, invitationId, configKey, file));
    }

    @GetMapping(
            "/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/template")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadOrganizationTemplate(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadOrganizationTemplate(
                        tenantId, organizationId, invitationId, configKey));
    }

    @GetMapping(
            "/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/file")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadOrganizationSignedDocument(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadOrganizationSignedDocument(
                        tenantId, organizationId, invitationId, configKey));
    }

    @PostMapping(
            "/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadOrganizationSignedDocument(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.uploadOrganizationSignedDocument(
                        tenantId, organizationId, invitationId, configKey, file));
    }

    @PostMapping(
            "/onboarding-invitations/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadRecordAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.uploadRecordAttachment(
                        tenantId, organizationId, invitationId, stepType, recordKey, file));
    }

    @PostMapping("/onboarding-invitations/{invitationId}/steps/DRIVING_LICENSE/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadDrivingLicenseAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.uploadDrivingLicenseAttachment(
                        tenantId, organizationId, invitationId, file));
    }

    @PostMapping("/onboarding-invitations/{invitationId}/steps/RANK/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadRankAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.uploadRankAttachment(
                        tenantId, organizationId, invitationId, file));
    }

    @DeleteMapping(
            "/onboarding-invitations/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> deleteRecordAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.deleteRecordAttachment(
                        tenantId,
                        organizationId,
                        invitationId,
                        stepType,
                        recordKey,
                        attachmentIndex));
    }

    @DeleteMapping("/onboarding-invitations/{invitationId}/steps/DRIVING_LICENSE/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> deleteDrivingLicenseAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.deleteDrivingLicenseAttachment(
                        tenantId, organizationId, invitationId, attachmentIndex));
    }

    @DeleteMapping("/onboarding-invitations/{invitationId}/steps/RANK/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> deleteRankAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return ResponseEntity.ok(
                adminOnboardingInvitationService.deleteRankAttachment(
                        tenantId, organizationId, invitationId, attachmentIndex));
    }

    @GetMapping(
            "/onboarding-invitations/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadRecordAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadRecordAttachment(
                        tenantId,
                        organizationId,
                        invitationId,
                        stepType,
                        recordKey,
                        attachmentIndex));
    }

    @GetMapping("/onboarding-invitations/{invitationId}/steps/DRIVING_LICENSE/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadDrivingLicenseAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadDrivingLicenseAttachment(
                        tenantId, organizationId, invitationId, attachmentIndex));
    }

    @GetMapping("/onboarding-invitations/{invitationId}/steps/RANK/attachment")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadRankAttachment(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                adminOnboardingInvitationService.downloadRankAttachment(
                        tenantId, organizationId, invitationId, attachmentIndex));
    }
}
