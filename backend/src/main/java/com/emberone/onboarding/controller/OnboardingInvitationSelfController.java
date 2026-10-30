/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.dto.SaveOnboardingStepRequest;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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
@RequestMapping("/api/v1/self/onboarding-invitations")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class OnboardingInvitationSelfController {

    private final OnboardingInvitationService onboardingInvitationService;
    private final OnboardingInvitationProgressService onboardingInvitationProgressService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Open onboarding invitations for the current user only")
    public ResponseEntity<List<SelfOnboardingInvitationDto>> listInvitations() {
        return ResponseEntity.ok(
                onboardingInvitationService.listAllForUser(tokenService.requireUserId()));
    }

    @GetMapping("/{invitationId}")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> getInvitation(
            @PathVariable long invitationId) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.getDetail(
                        tokenService.requireUserId(), invitationId));
    }

    @PutMapping("/{invitationId}/steps/{stepType}")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> saveStep(
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @Valid @RequestBody SaveOnboardingStepRequest request) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.saveStep(
                        tokenService.requireUserId(),
                        invitationId,
                        stepType,
                        request.getPayload()));
    }

    @GetMapping("/{invitationId}/documents/{configKey}/template")
    public ResponseEntity<InputStreamResource> downloadTemplate(
            @PathVariable long invitationId, @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadTemplate(
                        tokenService.requireUserId(), invitationId, configKey));
    }

    @GetMapping("/{invitationId}/documents/{configKey}/file")
    public ResponseEntity<InputStreamResource> downloadSignedDocument(
            @PathVariable long invitationId, @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadSignedDocument(
                        tokenService.requireUserId(), invitationId, configKey));
    }

    @PostMapping("/{invitationId}/documents/{configKey}")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadSignedDocument(
            @PathVariable long invitationId,
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.uploadSignedDocument(
                        tokenService.requireUserId(), invitationId, configKey, file));
    }

    @GetMapping("/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/template")
    public ResponseEntity<InputStreamResource> downloadOrganizationTemplate(
            @PathVariable long invitationId, @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadOrganizationTemplate(
                        tokenService.requireUserId(), invitationId, configKey));
    }

    @GetMapping("/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/file")
    public ResponseEntity<InputStreamResource> downloadOrganizationSignedDocument(
            @PathVariable long invitationId, @PathVariable String configKey) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadOrganizationSignedDocument(
                        tokenService.requireUserId(), invitationId, configKey));
    }

    @PostMapping("/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadOrganizationSignedDocument(
            @PathVariable long invitationId,
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.uploadOrganizationSignedDocument(
                        tokenService.requireUserId(), invitationId, configKey, file));
    }

    @PostMapping("/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadRecordAttachment(
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.uploadRecordAttachment(
                        tokenService.requireUserId(), invitationId, stepType, recordKey, file));
    }

    @PostMapping("/{invitationId}/steps/DRIVING_LICENSE/attachment")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadDrivingLicenseAttachment(
            @PathVariable long invitationId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.uploadDrivingLicenseAttachment(
                        tokenService.requireUserId(), invitationId, file));
    }

    @PostMapping("/{invitationId}/steps/RANK/attachment")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> uploadRankAttachment(
            @PathVariable long invitationId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.uploadRankAttachment(
                        tokenService.requireUserId(), invitationId, file));
    }

    @DeleteMapping("/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> deleteRecordAttachment(
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.deleteRecordAttachment(
                        tokenService.requireUserId(),
                        invitationId,
                        stepType,
                        recordKey,
                        attachmentIndex));
    }

    @DeleteMapping("/{invitationId}/steps/DRIVING_LICENSE/attachment")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> deleteDrivingLicenseAttachment(
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.deleteDrivingLicenseAttachment(
                        tokenService.requireUserId(), invitationId, attachmentIndex));
    }

    @DeleteMapping("/{invitationId}/steps/RANK/attachment")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> deleteRankAttachment(
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.deleteRankAttachment(
                        tokenService.requireUserId(), invitationId, attachmentIndex));
    }

    @GetMapping("/{invitationId}/steps/{stepType}/records/{recordKey}/attachment")
    public ResponseEntity<InputStreamResource> downloadRecordAttachment(
            @PathVariable long invitationId,
            @PathVariable OnboardingStepType stepType,
            @PathVariable String recordKey,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadRecordAttachment(
                        tokenService.requireUserId(),
                        invitationId,
                        stepType,
                        recordKey,
                        attachmentIndex));
    }

    @GetMapping("/{invitationId}/steps/RANK/attachment")
    public ResponseEntity<InputStreamResource> downloadRankAttachment(
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadRankAttachment(
                        tokenService.requireUserId(), invitationId, attachmentIndex));
    }

    @GetMapping("/{invitationId}/steps/DRIVING_LICENSE/attachment")
    public ResponseEntity<InputStreamResource> downloadDrivingLicenseAttachment(
            @PathVariable long invitationId,
            @RequestParam(defaultValue = "0") int attachmentIndex) {
        return OnboardingInvitationFileResponses.of(
                onboardingInvitationProgressService.downloadDrivingLicenseAttachment(
                        tokenService.requireUserId(), invitationId, attachmentIndex));
    }
}
