/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.user.dto.OrganizationMembershipSectionDto;
import com.emberone.user.service.UserMembershipOverviewService;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserMembershipOverviewControllerTest {

    @Mock UserMembershipOverviewService membershipOverviewService;
    @Mock UserService userService;

    @InjectMocks UserMembershipOverviewController controller;

    @Test
    void getMembershipOverviewReturnsOverview() {
        OrganizationMembershipSectionDto overview =
                OrganizationMembershipSectionDto.builder().build();
        when(membershipOverviewService.getMembershipOverview(1L, 10L)).thenReturn(overview);

        ResponseEntity<OrganizationMembershipSectionDto> response =
                controller.getMembershipOverview(1L, 10L);

        assertThat(response.getBody()).isEqualTo(overview);
        verify(membershipOverviewService).getMembershipOverview(1L, 10L);
        verify(userService).assertUserBelongsToOrganization(10L, 1L);
    }

    @Test
    void getMembershipOverviewRejectsNonMemberWithoutLoadingOverview() {
        doThrow(new IllegalArgumentException("User does not belong to the specified organization"))
                .when(userService)
                .assertUserBelongsToOrganization(10L, 1L);

        assertThatThrownBy(() -> controller.getMembershipOverview(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(membershipOverviewService);
    }

    @Test
    void getMainOrganizationChangeTargetsRejectsNonMember() {
        doThrow(new IllegalArgumentException("User does not belong to the specified organization"))
                .when(userService)
                .assertUserBelongsToOrganization(10L, 1L);

        assertThatThrownBy(() -> controller.getMainOrganizationChangeTargets(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userService, never()).getMainOrganizationChangeTargetsForMember(1L, 10L);
    }
}
