/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.list.TimeEntryListFilter;
import com.emberone.timeentry.service.UserTimeEntryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserTimeEntryOrganizationControllerTest {

    @Mock UserTimeEntryService userTimeEntryService;

    @InjectMocks UserTimeEntryOrganizationController controller;

    @Test
    void getTimeEntryDelegatesToService() {
        UserTimeEntryDto dto = UserTimeEntryDto.builder().id(2L).build();
        when(userTimeEntryService.getTimeEntryForOrganization(10L, 2L)).thenReturn(dto);

        ResponseEntity<UserTimeEntryDto> response = controller.getTimeEntry(10L, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void listTimeEntriesDelegatesToService() {
        UserTimeEntryPageDto page =
                UserTimeEntryPageDto.builder()
                        .content(List.of(UserTimeEntryDto.builder().id(2L).build()))
                        .build();
        ListQuery query = new ListQuery(0, null, null, null);
        TimeEntryListFilter filter = TimeEntryListFilter.forRange(null, null);
        when(userTimeEntryService.listTimeEntriesForOrganization(10L, query, filter))
                .thenReturn(page);

        ResponseEntity<UserTimeEntryPageDto> response =
                controller.listTimeEntries(10L, query, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
        verify(userTimeEntryService).listTimeEntriesForOrganization(10L, query, filter);
    }
}
