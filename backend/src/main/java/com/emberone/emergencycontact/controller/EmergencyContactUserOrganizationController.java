/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.controller;

import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.dto.ReorderEmergencyContactsRequest;
import com.emberone.emergencycontact.dto.UpdateEmergencyContactRequest;
import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactUserOrganizationController {

    private final EmergencyContactService emergencyContactService;
    private final UserService userService;

    @GetMapping
    @BoundedList("Prioritized per-user sub-resource; cardinality stays small by design.")
    @PreAuthorize("@securityService.hasEmergencyContactReadPermission(#organizationId)")
    public ResponseEntity<List<EmergencyContactDto>> listEmergencyContacts(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(emergencyContactService.getEmergencyContactsForUser(userId));
    }

    @PostMapping
    @PreAuthorize("@securityService.hasEmergencyContactWritePermission(#organizationId)")
    public ResponseEntity<EmergencyContactDto> createEmergencyContact(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateEmergencyContactRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        EmergencyContactDto created =
                emergencyContactService.createEmergencyContact(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/reorder")
    @PreAuthorize("@securityService.hasEmergencyContactWritePermission(#organizationId)")
    public ResponseEntity<List<EmergencyContactDto>> reorderEmergencyContacts(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody ReorderEmergencyContactsRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(emergencyContactService.reorderEmergencyContacts(userId, request));
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("@securityService.hasEmergencyContactWritePermission(#organizationId)")
    public ResponseEntity<EmergencyContactDto> updateEmergencyContact(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateEmergencyContactRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                emergencyContactService.updateEmergencyContact(userId, contactId, request));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("@securityService.hasEmergencyContactWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteEmergencyContact(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long contactId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        emergencyContactService.deleteEmergencyContact(userId, contactId);
        return ResponseEntity.noContent().build();
    }
}
