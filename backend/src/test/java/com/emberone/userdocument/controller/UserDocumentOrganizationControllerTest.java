/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.userdocument.dto.UserDocumentDto;
import com.emberone.userdocument.dto.UserDocumentPageDto;
import com.emberone.userdocument.list.UserDocumentListFilter;
import com.emberone.userdocument.list.UserDocumentListFilterOptions;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserDocumentOrganizationControllerTest {

    @Mock UserDocumentService userDocumentService;
    @Mock UserDocumentListFilterOptions userDocumentListFilterOptions;
    @Mock MultipartFile file;

    @InjectMocks UserDocumentOrganizationController controller;

    @Test
    void listFilterOptionsAssertsUserBelongsToOrganization() {
        ListFilterOptionsDto options = new ListFilterOptionsDto(Map.of());
        when(userDocumentListFilterOptions.forUser(7L)).thenReturn(options);

        ResponseEntity<ListFilterOptionsDto> response = controller.listFilterOptions(10L, 7L);

        verify(userDocumentService).assertUserBelongsToOrganization(7L, 10L);
        assertThat(response.getBody()).isEqualTo(options);
    }

    @Test
    void listDocumentsAssertsUserBelongsToOrganization() {
        UserDocumentPageDto page =
                UserDocumentPageDto.builder()
                        .content(List.of(UserDocumentDto.builder().id(1L).build()))
                        .page(0)
                        .size(50)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .build();
        ListQuery query = new ListQuery(0, null, null, null);
        UserDocumentListFilter filter = new UserDocumentListFilter("contract", null, null);
        when(userDocumentService.listDocuments(7L, query, filter)).thenReturn(page);

        ResponseEntity<UserDocumentPageDto> response =
                controller.listDocuments(10L, 7L, query, filter);

        verify(userDocumentService).assertUserBelongsToOrganization(7L, 10L);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void uploadDocumentReturnsCreated() {
        UserDocumentDto created = UserDocumentDto.builder().id(2L).build();
        when(userDocumentService.uploadDocument(7L, file, "title", "desc", List.of("tag")))
                .thenReturn(created);

        ResponseEntity<UserDocumentDto> response =
                controller.uploadDocument(10L, 7L, file, "title", "desc", List.of("tag"));

        verify(userDocumentService).assertUserBelongsToOrganization(7L, 10L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }
}
