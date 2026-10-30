/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboardingdocument.controller;

import com.emberone.onboardingdocument.dto.CreateTenantOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.dto.TenantOnboardingDocumentDefinitionDto;
import com.emberone.onboardingdocument.dto.UpdateTenantOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.TenantOnboardingDocumentDefinitionService;
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
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/onboarding-documents")
@RequiredArgsConstructor
public class AdminTenantOnboardingDocumentController {

    private final TenantOnboardingDocumentDefinitionService definitionService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Tenant onboarding document definition catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantOnboardingDocumentDefinitionDto>> list(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(definitionService.list(tenantId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantOnboardingDocumentDefinitionDto> create(
            @PathVariable long tenantId,
            @Valid @RequestBody CreateTenantOnboardingDocumentDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.create(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantOnboardingDocumentDefinitionDto> update(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantOnboardingDocumentDefinitionRequest request) {
        return ResponseEntity.ok(definitionService.update(tenantId, configKey, request));
    }

    @PostMapping("/{configKey}/template")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantOnboardingDocumentDefinitionDto> uploadTemplate(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                definitionService.uploadTemplate(
                        tenantId, configKey, file, tokenService.getReferencedUserId()));
    }

    @GetMapping("/{configKey}/template/download")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<InputStreamResource> downloadTemplate(
            @PathVariable long tenantId, @PathVariable String configKey) {
        OnboardingDocumentTemplateService.TemplateDownload download =
                definitionService.downloadTemplate(tenantId, configKey);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(download.fileName()))
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(new InputStreamResource(download.inputStream()));
    }

    @DeleteMapping("/{configKey}/template")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantOnboardingDocumentDefinitionDto> deleteTemplate(
            @PathVariable long tenantId, @PathVariable String configKey) {
        return ResponseEntity.ok(definitionService.deleteTemplate(tenantId, configKey));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> delete(
            @PathVariable long tenantId, @PathVariable String configKey) {
        definitionService.delete(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        definitionService.archive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        definitionService.unarchive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
