/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.platform.http.ContentDispositionHeaders;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class OnboardingInvitationFileResponses {

    private OnboardingInvitationFileResponses() {}

    public static ResponseEntity<InputStreamResource> of(
            OnboardingDocumentTemplateService.TemplateDownload download) {
        return of(download.fileName(), download.contentType(), download.inputStream());
    }

    public static ResponseEntity<InputStreamResource> of(
            OrganizationOnboardingDocumentTemplateService.TemplateDownload download) {
        return of(download.fileName(), download.contentType(), download.inputStream());
    }

    public static ResponseEntity<InputStreamResource> of(
            OnboardingInvitationProgressService.StoredUpload download) {
        return of(download.fileName(), download.contentType(), download.inputStream());
    }

    private static ResponseEntity<InputStreamResource> of(
            String fileName, String contentType, java.io.InputStream inputStream) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(fileName))
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(inputStream));
    }
}
