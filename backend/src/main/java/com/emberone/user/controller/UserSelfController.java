/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import com.emberone.event.dto.OperationAlarmAttendanceStatsDto;
import com.emberone.event.dto.ServiceHoursTotalDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.organization.dto.OrganizationTreeDto;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.user.dto.SelfMembershipHistoryDto;
import com.emberone.user.dto.UpdateContactInformationRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.dto.VerifyNotificationEmailRequest;
import com.emberone.user.service.SelfMembershipHistoryService;
import com.emberone.user.service.ServiceHoursTotalService;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/self")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserSelfController {

    private final UserService userService;
    private final EventAttendanceService eventAttendanceService;
    private final ServiceHoursTotalService serviceHoursTotalService;
    private final SelfMembershipHistoryService selfMembershipHistoryService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<UserDto> getUserData() {
        UserDto userDto = userService.getUserById(tokenService.requireUserId());
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/organizations/tree")
    @BoundedList("Organization tree for current user memberships")
    public ResponseEntity<List<OrganizationTreeDto>> getOrganizationTree() {
        List<OrganizationTreeDto> tree =
                userService.getOrganizationTreeForUser(tokenService.requireUserId());
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/organizations/memberships")
    @BoundedList("Organization and group memberships of the current user, including past ones")
    public ResponseEntity<SelfMembershipHistoryDto> getMembershipHistory() {
        return ResponseEntity.ok(
                selfMembershipHistoryService.getForUser(tokenService.requireUserId()));
    }

    @GetMapping("/service-hours-total")
    public ResponseEntity<ServiceHoursTotalDto> getServiceHoursTotal(@RequestParam int year) {
        return ResponseEntity.ok(
                serviceHoursTotalService.getForUser(tokenService.requireUserId(), year));
    }

    @GetMapping("/operation-alarm-attendance-stats")
    public ResponseEntity<OperationAlarmAttendanceStatsDto> getOperationAlarmAttendanceStats() {
        return ResponseEntity.ok(
                eventAttendanceService.getOperationAlarmAttendanceStatsForUser(
                        tokenService.requireUserId()));
    }

    @PutMapping("/notification-email")
    public ResponseEntity<UserDto> updateNotificationEmail(
            @Valid @RequestBody UpdateContactInformationRequest request) {
        UserDto userDto =
                userService.updateContactInformationSelf(tokenService.requireUserId(), request);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/notification-email/verify")
    public ResponseEntity<UserDto> verifyNotificationEmail(
            @Valid @RequestBody VerifyNotificationEmailRequest request) {
        UserDto userDto =
                userService.verifyNotificationEmailSelf(
                        tokenService.requireUserId(), request.getToken());
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/notification-email/resend")
    public ResponseEntity<UserDto> resendNotificationEmailVerification() {
        UserDto userDto =
                userService.resendNotificationEmailVerificationSelf(tokenService.requireUserId());
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/notification-email/pending")
    public ResponseEntity<UserDto> cancelPendingNotificationEmail() {
        UserDto userDto =
                userService.cancelPendingNotificationEmailSelf(tokenService.requireUserId());
        return ResponseEntity.ok(userDto);
    }
}
