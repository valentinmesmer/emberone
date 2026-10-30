/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.controller;

import com.emberone.management.dto.ManagementActivationDto;
import com.emberone.management.dto.ManagementActivationRequest;
import com.emberone.management.dto.ManagementTargetOrganizationDto;
import com.emberone.management.service.ManagementAccessService;
import com.emberone.management.service.ManagementActivationService;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/self/management")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class ManagementSelfController {

    private final ManagementActivationService activationService;
    private final ManagementAccessService accessService;
    private final TokenService tokenService;

    @GetMapping("/organizations")
    @BoundedList("Organizations the current user may open through management")
    public ResponseEntity<List<ManagementTargetOrganizationDto>> listTargets() {
        return ResponseEntity.ok(
                activationService.listTargets(
                        tokenService.requireTenantId(), tokenService.requireUserId()));
    }

    /** Master data of a managed target. Only with live rights in it, else not found. */
    @GetMapping("/organizations/{organizationId}")
    public ResponseEntity<OrganizationDto> getTarget(@PathVariable Long organizationId) {
        if (accessService.rightsIn(organizationId).isEmpty()) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return ResponseEntity.ok(
                activationService.openTarget(
                        tokenService.requireTenantId(),
                        tokenService.requireUserId(),
                        organizationId));
    }

    @GetMapping("/activations")
    @BoundedList("Open management activations of the current user")
    public ResponseEntity<List<ManagementActivationDto>> listActivations() {
        return ResponseEntity.ok(
                activationService.listOwnOpen(
                        tokenService.requireTenantId(), tokenService.requireUserId()));
    }

    @PostMapping("/activations")
    public ResponseEntity<ManagementActivationDto> activate(
            @Valid @RequestBody ManagementActivationRequest request) {
        ManagementActivationDto created =
                activationService.activate(
                        tokenService.requireTenantId(), tokenService.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/activations/{activationId}")
    public ResponseEntity<Void> end(@PathVariable Long activationId) {
        activationService.endOwn(
                tokenService.requireTenantId(), tokenService.requireUserId(), activationId);
        return ResponseEntity.noContent().build();
    }
}
