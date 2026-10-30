/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.userdocument.dto.UserDocumentDto;
import com.emberone.userdocument.dto.UserDocumentPageDto;
import com.emberone.userdocument.list.UserDocumentListFilter;
import com.emberone.userdocument.list.UserDocumentSelfListFilterOptions;
import com.emberone.userdocument.service.UserDocumentService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserDocumentSelfControllerTest {

    @Mock UserDocumentService userDocumentService;
    @Mock TokenService tokenService;
    @Mock UserDocumentSelfListFilterOptions selfListFilterOptions;

    UserDocumentSelfController controller;

    @BeforeEach
    void setUp() {
        controller =
                new UserDocumentSelfController(
                        userDocumentService, tokenService, selfListFilterOptions);
    }

    @Test
    void listFilterOptionsDelegatesToSource() {
        ListFilterOptionsDto options = new ListFilterOptionsDto(Map.of());
        when(selfListFilterOptions.filterOptions()).thenReturn(options);

        ResponseEntity<ListFilterOptionsDto> response = controller.listFilterOptions();

        assertThat(response.getBody()).isEqualTo(options);
    }

    @Test
    void listDocumentsDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(5L);
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
        UserDocumentListFilter filter = new UserDocumentListFilter("hr", null, null);
        when(userDocumentService.listDocuments(5L, query, filter)).thenReturn(page);

        ResponseEntity<UserDocumentPageDto> response = controller.listDocuments(query, filter);

        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void markAllViewedDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(5L);

        ResponseEntity<Void> response = controller.markAllViewed();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        org.mockito.Mockito.verify(userDocumentService).markAllViewed(5L);
    }

    @Test
    void downloadDocumentSetsAttachmentDisposition() {
        when(tokenService.requireUserId()).thenReturn(5L);
        InputStreamResource resource =
                new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        when(userDocumentService.downloadDocument(5L, 3L))
                .thenReturn(
                        new UserDocumentService.DocumentDownload(
                                resource, "doc.pdf", "application/pdf"));

        ResponseEntity<InputStreamResource> response = controller.downloadDocument(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("doc.pdf");
    }
}
