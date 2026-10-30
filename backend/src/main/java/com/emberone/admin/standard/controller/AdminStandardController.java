/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.controller;

import com.emberone.admin.onboarding.dto.OnboardingStepTypesResponse;
import com.emberone.admin.onboarding.dto.OnboardingStepsResponse;
import com.emberone.admin.onboarding.dto.ReplaceOnboardingStepsRequest;
import com.emberone.admin.standard.dto.*;
import com.emberone.admin.standard.service.AdminStandardService;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Standards library admin API: list/clone/update/delete standards, assign a standard per
 * organization type, and preview membership impact before removals or standard switches. Standard
 * template <em>content</em> editing lives in the per-domain admin endpoints (organization groups,
 * permission groups, workflow definitions/mappings).
 */
@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/standards")
@RequiredArgsConstructor
public class AdminStandardController {

    private final AdminStandardService adminStandardService;

    @GetMapping
    @BoundedList("Tenant standards library; cardinality stays small per tenant")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<StandardDto>> listStandards(@PathVariable long tenantId) {
        return ResponseEntity.ok(adminStandardService.listStandards(tenantId));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardDto> cloneStandard(
            @PathVariable long tenantId, @Valid @RequestBody CreateStandardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminStandardService.cloneStandard(tenantId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardDto> updateStandard(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStandardRequest request) {
        return ResponseEntity.ok(adminStandardService.updateStandard(tenantId, id, request));
    }

    @GetMapping("/{id}/content")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> getContent(
            @PathVariable long tenantId, @PathVariable Long id) {
        return ResponseEntity.ok(adminStandardService.getContent(tenantId, id));
    }

    @GetMapping("/{id}/modules")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardModulesDto> getEnabledModules(
            @PathVariable long tenantId, @PathVariable Long id) {
        return ResponseEntity.ok(adminStandardService.getEnabledModules(tenantId, id));
    }

    @PutMapping("/{id}/modules")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardModulesDto> updateEnabledModules(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody StandardModulesDto request) {
        return ResponseEntity.ok(adminStandardService.updateEnabledModules(tenantId, id, request));
    }

    // --- Content editing (custom standards)
    // --------------------------------------------------------

    @PostMapping("/{id}/organization-groups")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> addOrgGroup(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody CreateOrgGroupRequest request) {
        return ResponseEntity.ok(adminStandardService.addOrgGroup(tenantId, id, request));
    }

    @PutMapping("/{id}/organization-groups/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateOrgGroup(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateOrgGroupRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateOrgGroup(tenantId, id, configKey, request));
    }

    @DeleteMapping("/{id}/organization-groups/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> deleteOrgGroup(
            @PathVariable long tenantId, @PathVariable Long id, @PathVariable String configKey) {
        return ResponseEntity.ok(adminStandardService.deleteOrgGroup(tenantId, id, configKey));
    }

    @GetMapping("/{id}/organization-groups/{configKey}/removal-impact")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrgGroupRemovalImpactDto> getOrgGroupRemovalImpact(
            @PathVariable long tenantId, @PathVariable Long id, @PathVariable String configKey) {
        return ResponseEntity.ok(
                adminStandardService.getOrgGroupRemovalImpact(tenantId, id, configKey));
    }

    @PostMapping("/{id}/permission-groups")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> addPermissionGroup(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody CreatePermissionGroupRequest request) {
        return ResponseEntity.ok(adminStandardService.addPermissionGroup(tenantId, id, request));
    }

    @PutMapping("/{id}/permission-groups/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updatePermissionGroup(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @PathVariable String configKey,
            @Valid @RequestBody UpdatePermissionGroupRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updatePermissionGroup(tenantId, id, configKey, request));
    }

    @DeleteMapping("/{id}/permission-groups/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> deletePermissionGroup(
            @PathVariable long tenantId, @PathVariable Long id, @PathVariable String configKey) {
        return ResponseEntity.ok(
                adminStandardService.deletePermissionGroup(tenantId, id, configKey));
    }

    @PostMapping("/{id}/group-mappings")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> addGroupMapping(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody CreateGroupMappingRequest request) {
        return ResponseEntity.ok(adminStandardService.addGroupMapping(tenantId, id, request));
    }

    @DeleteMapping("/{id}/group-mappings/{orgGroupConfigKey}/{permissionGroupConfigKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> deleteGroupMapping(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @PathVariable String orgGroupConfigKey,
            @PathVariable String permissionGroupConfigKey) {
        return ResponseEntity.ok(
                adminStandardService.deleteGroupMapping(
                        tenantId, id, orgGroupConfigKey, permissionGroupConfigKey));
    }

    @PutMapping("/{id}/workflows/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateWorkflow(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateWorkflowRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateWorkflow(tenantId, id, configKey, request));
    }

    // --- Certifications (selection only) ---------------------------------------------------------

    @PutMapping("/{id}/certification-selections")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateCertificationSelections(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCertificationSelectionsRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateCertificationSelections(tenantId, id, request));
    }

    @PutMapping("/{id}/training-course-type-selections")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateTrainingCourseTypeSelections(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrainingCourseTypeSelectionsRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateTrainingCourseTypeSelections(tenantId, id, request));
    }

    @PutMapping("/{id}/driving-license-selections")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateDrivingLicenseSelections(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDrivingLicenseSelectionsRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateDrivingLicenseSelections(tenantId, id, request));
    }

    @PutMapping("/{id}/qualification-selections")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateQualificationSelections(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateQualificationSelectionsRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateQualificationSelections(tenantId, id, request));
    }

    @PutMapping("/{id}/rank-selections")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateRankSelections(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRankSelectionsRequest request) {
        return ResponseEntity.ok(adminStandardService.updateRankSelections(tenantId, id, request));
    }

    @PutMapping("/{id}/function-selections")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateFunctionSelections(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFunctionSelectionsRequest request) {
        return ResponseEntity.ok(
                adminStandardService.updateFunctionSelections(tenantId, id, request));
    }

    @PutMapping("/{id}/function-grants")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardContentDto> updateFunctionGrants(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFunctionGrantsRequest request) {
        return ResponseEntity.ok(adminStandardService.updateFunctionGrants(tenantId, id, request));
    }

    @GetMapping("/onboarding-step-types")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OnboardingStepTypesResponse> listOnboardingStepTypes(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(adminStandardService.listOnboardingStepTypes(tenantId));
    }

    @GetMapping("/{id}/onboarding-steps")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OnboardingStepsResponse> getOnboardingSteps(
            @PathVariable long tenantId, @PathVariable Long id) {
        return ResponseEntity.ok(adminStandardService.getOnboardingSteps(tenantId, id));
    }

    @PutMapping("/{id}/onboarding-steps")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OnboardingStepsResponse> replaceOnboardingSteps(
            @PathVariable long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody ReplaceOnboardingStepsRequest request) {
        return ResponseEntity.ok(
                adminStandardService.replaceOnboardingSteps(tenantId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> deleteStandard(@PathVariable long tenantId, @PathVariable Long id) {
        adminStandardService.deleteStandard(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assignments")
    @BoundedList("One assignment per organization type of the tenant")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<StandardAssignmentDto>> listAssignments(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(adminStandardService.listAssignments(tenantId));
    }

    @PutMapping("/org-types/{organizationTypeId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardAssignmentDto> assignStandard(
            @PathVariable long tenantId,
            @PathVariable Long organizationTypeId,
            @Valid @RequestBody AssignStandardRequest request) {
        return ResponseEntity.ok(
                adminStandardService.assignStandard(tenantId, organizationTypeId, request));
    }

    @GetMapping("/org-types/{organizationTypeId}/assignment-impact")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardSwitchImpactDto> getAssignmentImpact(
            @PathVariable long tenantId,
            @PathVariable Long organizationTypeId,
            @RequestParam Long standardId) {
        return ResponseEntity.ok(
                adminStandardService.getAssignmentImpact(tenantId, organizationTypeId, standardId));
    }

    // --- Per-organization standard override
    // --------------------------------------------------------

    @GetMapping("/organization-overrides")
    @BoundedList("Organizations with an explicit standard override; configuration list")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<OrganizationStandardOverrideDto>> listOrganizationOverrides(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(adminStandardService.listOrganizationOverrides(tenantId));
    }

    @GetMapping("/organizations/{organizationId}/standard")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationStandardDto> getOrganizationStandard(
            @PathVariable long tenantId, @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                adminStandardService.getOrganizationStandard(tenantId, organizationId));
    }

    @PutMapping("/organizations/{organizationId}/standard")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<OrganizationStandardDto> setOrganizationStandard(
            @PathVariable long tenantId,
            @PathVariable Long organizationId,
            @Valid @RequestBody SetOrganizationStandardRequest request) {
        return ResponseEntity.ok(
                adminStandardService.setOrganizationStandard(tenantId, organizationId, request));
    }

    @GetMapping("/organizations/{organizationId}/standard-impact")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<StandardSwitchImpactDto> getOrganizationStandardImpact(
            @PathVariable long tenantId,
            @PathVariable Long organizationId,
            @RequestParam(required = false) Long standardId) {
        return ResponseEntity.ok(
                adminStandardService.getOrganizationStandardImpact(
                        tenantId, organizationId, standardId));
    }
}
