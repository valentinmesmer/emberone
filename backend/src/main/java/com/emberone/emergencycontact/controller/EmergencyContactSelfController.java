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
@RequestMapping("/api/v1/self/emergency-contacts")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class EmergencyContactSelfController {

    private final EmergencyContactService emergencyContactService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Prioritized per-user sub-resource; cardinality stays small by design.")
    public ResponseEntity<List<EmergencyContactDto>> getEmergencyContacts() {
        return ResponseEntity.ok(
                emergencyContactService.getEmergencyContactsForUser(tokenService.requireUserId()));
    }

    @PostMapping
    public ResponseEntity<EmergencyContactDto> createEmergencyContact(
            @Valid @RequestBody CreateEmergencyContactRequest request) {
        EmergencyContactDto created =
                emergencyContactService.createEmergencyContact(
                        tokenService.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<EmergencyContactDto>> reorderEmergencyContacts(
            @Valid @RequestBody ReorderEmergencyContactsRequest request) {
        List<EmergencyContactDto> reordered =
                emergencyContactService.reorderEmergencyContacts(
                        tokenService.requireUserId(), request);
        return ResponseEntity.ok(reordered);
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<EmergencyContactDto> updateEmergencyContact(
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateEmergencyContactRequest request) {
        EmergencyContactDto updated =
                emergencyContactService.updateEmergencyContact(
                        tokenService.requireUserId(), contactId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteEmergencyContact(@PathVariable Long contactId) {
        emergencyContactService.deleteEmergencyContact(tokenService.requireUserId(), contactId);
        return ResponseEntity.noContent().build();
    }
}
