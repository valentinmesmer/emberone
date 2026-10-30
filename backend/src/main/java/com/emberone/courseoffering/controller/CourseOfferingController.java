/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.CancelCourseOfferingRequest;
import com.emberone.courseoffering.dto.CourseAudienceOptionsDto;
import com.emberone.courseoffering.dto.CourseOfferingDto;
import com.emberone.courseoffering.dto.CourseOfferingPageDto;
import com.emberone.courseoffering.dto.CourseSessionRequest;
import com.emberone.courseoffering.dto.CreateCourseOfferingRequest;
import com.emberone.courseoffering.dto.InstantiateCourseOfferingTemplateRequest;
import com.emberone.courseoffering.dto.ReplaceCourseAudienceRequest;
import com.emberone.courseoffering.dto.ReplaceCoursePrerequisitesRequest;
import com.emberone.courseoffering.dto.SaveAsCourseOfferingTemplateRequest;
import com.emberone.courseoffering.dto.UpdateCourseOfferingRequest;
import com.emberone.courseoffering.list.CourseOfferingListFilter;
import com.emberone.courseoffering.list.CourseOfferingOrganizationListFilterOptions;
import com.emberone.courseoffering.service.CourseAudienceOptionsService;
import com.emberone.courseoffering.service.CourseOfferingCancellationService;
import com.emberone.courseoffering.service.CourseOfferingCatalogService;
import com.emberone.courseoffering.service.CourseOfferingService;
import com.emberone.courseoffering.service.CourseOfferingTemplateService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Course offerings of an organization (offering side). */
@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/course-offerings")
@RequiredArgsConstructor
public class CourseOfferingController {

    private final CourseOfferingService offeringService;
    private final CourseOfferingCatalogService catalogService;
    private final CourseAudienceOptionsService audienceOptionsService;
    private final CourseOfferingTemplateService templateService;
    private final CourseOfferingCancellationService cancellationService;
    private final CourseOfferingOrganizationListFilterOptions organizationListFilterOptions;

    @GetMapping
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseOfferingPageDto> list(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> trainingCourseTypeId) {
        return ResponseEntity.ok(
                offeringService.list(
                        organizationId,
                        query,
                        CourseOfferingListFilter.parse(status, trainingCourseTypeId)));
    }

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(organizationListFilterOptions.forOrganization(organizationId));
    }

    @GetMapping("/templates")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseOfferingPageDto> listTemplates(
            @PathVariable Long organizationId, ListQuery query) {
        return ResponseEntity.ok(offeringService.listTemplates(organizationId, query));
    }

    @GetMapping("/offerable-types")
    @BoundedList("Org-standard course types the organization may offer")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<List<TrainingCourseTypeDto>> listOfferableTypes(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(catalogService.listOfferableTypes(organizationId));
    }

    /**
     * Active organizations and organization types of the tenant for audience pickers ({@code ORG},
     * {@code ORG_SUBTREE}, {@code ORG_TYPE}). Only id and name.
     */
    @GetMapping("/audience-options")
    @BoundedList("Tenant organizations and organization types for audience pickers")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseAudienceOptionsDto> listAudienceOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(audienceOptionsService.listForOrganization(organizationId));
    }

    @GetMapping("/{offeringId}")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> get(
            @PathVariable Long organizationId, @PathVariable Long offeringId) {
        return ResponseEntity.ok(offeringService.get(organizationId, offeringId));
    }

    @PostMapping
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> create(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateCourseOfferingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offeringService.create(organizationId, request));
    }

    @PostMapping("/{offeringId}/save-as-template")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> saveAsTemplate(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody SaveAsCourseOfferingTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        templateService.saveAsTemplate(
                                organizationId, offeringId, request.getTemplateName()));
    }

    @PostMapping("/{offeringId}/instantiate")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> instantiate(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody InstantiateCourseOfferingTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        templateService.instantiate(
                                organizationId, offeringId, request.getFirstSessionDate()));
    }

    @PutMapping("/{offeringId}")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> update(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody UpdateCourseOfferingRequest request) {
        return ResponseEntity.ok(offeringService.update(organizationId, offeringId, request));
    }

    @DeleteMapping("/{offeringId}")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<Void> delete(
            @PathVariable Long organizationId, @PathVariable Long offeringId) {
        offeringService.delete(organizationId, offeringId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{offeringId}/publish")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> publish(
            @PathVariable Long organizationId, @PathVariable Long offeringId) {
        return ResponseEntity.ok(offeringService.publish(organizationId, offeringId));
    }

    @PostMapping("/{offeringId}/cancel")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> cancel(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody CancelCourseOfferingRequest request) {
        return ResponseEntity.ok(
                cancellationService.cancel(organizationId, offeringId, request.getReason()));
    }

    @PutMapping("/{offeringId}/audiences")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> replaceAudiences(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody ReplaceCourseAudienceRequest request) {
        return ResponseEntity.ok(
                offeringService.replaceAudiences(organizationId, offeringId, request.getEntries()));
    }

    @PutMapping("/{offeringId}/prerequisites")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> replacePrerequisites(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody ReplaceCoursePrerequisitesRequest request) {
        return ResponseEntity.ok(
                offeringService.replacePrerequisites(organizationId, offeringId, request));
    }

    @PostMapping("/{offeringId}/sessions")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> addSession(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @Valid @RequestBody CourseSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offeringService.addSession(organizationId, offeringId, request));
    }

    @PutMapping("/{offeringId}/sessions/{sessionId}")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> updateSession(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @PathVariable Long sessionId,
            @Valid @RequestBody CourseSessionRequest request) {
        return ResponseEntity.ok(
                offeringService.updateSession(organizationId, offeringId, sessionId, request));
    }

    @DeleteMapping("/{offeringId}/sessions/{sessionId}")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseOfferingDto> deleteSession(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(
                offeringService.deleteSession(organizationId, offeringId, sessionId));
    }
}
