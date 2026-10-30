/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboardingdocument.controller;

import com.emberone.onboardingdocument.dto.CreateOrganizationOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.dto.OrganizationOnboardingDocumentDefinitionDto;
import com.emberone.onboardingdocument.dto.UpdateOrganizationOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentDefinitionService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.platform.http.ContentDispositionHeaders;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(
        "/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/onboarding-documents")
@RequiredArgsConstructor
public class AdminOrganizationOnboardingDocumentController {

    private final OrganizationOnboardingDocumentDefinitionService definitionService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Organization onboarding document definition catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<OrganizationOnboardingDocumentDefinitionDto>> list(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(definitionService.list(tenantId, organizationId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationOnboardingDocumentDefinitionDto> create(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody CreateOrganizationOnboardingDocumentDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.create(tenantId, organizationId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationOnboardingDocumentDefinitionDto> update(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateOrganizationOnboardingDocumentDefinitionRequest request) {
        return ResponseEntity.ok(
                definitionService.update(tenantId, organizationId, configKey, request));
    }

    @PostMapping("/{configKey}/template")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationOnboardingDocumentDefinitionDto> uploadTemplate(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                definitionService.uploadTemplate(
                        tenantId,
                        organizationId,
                        configKey,
                        file,
                        tokenService.getReferencedUserId()));
    }

    @GetMapping("/{configKey}/template/download")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadTemplate(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey) {
        OrganizationOnboardingDocumentTemplateService.TemplateDownload download =
                definitionService.downloadTemplate(tenantId, organizationId, configKey);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(download.fileName()))
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(new InputStreamResource(download.inputStream()));
    }

    @DeleteMapping("/{configKey}/template")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationOnboardingDocumentDefinitionDto> deleteTemplate(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey) {
        return ResponseEntity.ok(
                definitionService.deleteTemplate(tenantId, organizationId, configKey));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> delete(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey) {
        definitionService.delete(tenantId, organizationId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey) {
        definitionService.archive(tenantId, organizationId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @PathVariable String configKey) {
        definitionService.unarchive(tenantId, organizationId, configKey);
        return ResponseEntity.noContent().build();
    }
}
