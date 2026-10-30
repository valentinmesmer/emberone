/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import com.emberone.absence.list.AbsenceListFilter;
import com.emberone.absence.list.AbsenceOrganizationListFilterOptions;
import com.emberone.absence.list.AbsencePeriod;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserAbsenceOrganizationControllerTest {

    @Mock UserAbsenceService userAbsenceService;

    @Mock AbsenceOrganizationListFilterOptions organizationListFilterOptions;

    @InjectMocks UserAbsenceOrganizationController controller;

    @Test
    void getActiveAbsencesForOrganizationDelegatesToService() {
        UserAbsencePageDto page =
                UserAbsencePageDto.builder()
                        .content(List.of(UserAbsenceDto.builder().id(3L).build()))
                        .build();
        ListQuery query = new ListQuery(0, null, null, null);
        AbsenceListFilter filter =
                AbsenceListFilter.parseOrganizationActiveList(
                        List.of(AbsencePeriod.CURRENT.name()), List.of(), List.of());
        when(userAbsenceService.listActiveAbsencesForOrganization(10L, query, filter))
                .thenReturn(page);

        ResponseEntity<UserAbsencePageDto> response =
                controller.getActiveAbsencesForOrganization(
                        10L, query, List.of(AbsencePeriod.CURRENT.name()), List.of(), List.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
        verify(userAbsenceService).listActiveAbsencesForOrganization(10L, query, filter);
    }
}
