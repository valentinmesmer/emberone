/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.service.AppointmentDocumentService;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.platform.security.TokenService;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AppointmentDocumentControllerTest {

    @Mock AppointmentDocumentService appointmentDocumentService;
    @Mock TokenService tokenService;
    @Mock MultipartFile file;

    AppointmentDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new AppointmentDocumentController(appointmentDocumentService, tokenService);
    }

    @Test
    void listAttachmentsDelegatesToService() {
        List<EntityAttachmentDto> attachments =
                List.of(EntityAttachmentDto.builder().id(1L).build());
        when(appointmentDocumentService.listAttachments(10L, 5L)).thenReturn(attachments);

        ResponseEntity<List<EntityAttachmentDto>> response = controller.listAttachments(10L, 5L);

        assertThat(response.getBody()).isEqualTo(attachments);
    }

    @Test
    void uploadAttachmentThrowsWhenUserIdMissing() {
        when(tokenService.getUserId()).thenReturn(null);

        assertThatThrownBy(() -> controller.uploadAttachment(10L, 5L, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User ID is required");
    }

    @Test
    void uploadAttachmentDelegatesToService() {
        when(tokenService.getUserId()).thenReturn(3L);
        EntityAttachmentDto created = EntityAttachmentDto.builder().id(2L).build();
        when(appointmentDocumentService.uploadAttachment(3L, 10L, 5L, file)).thenReturn(created);

        ResponseEntity<EntityAttachmentDto> response = controller.uploadAttachment(10L, 5L, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void downloadAttachmentSetsContentDisposition() {
        InputStreamResource resource =
                new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        when(appointmentDocumentService.downloadAttachment(10L, 5L, 2L)).thenReturn(resource);
        when(appointmentDocumentService.getFileName(10L, 5L, 2L)).thenReturn("notes.pdf");
        when(appointmentDocumentService.getContentType(10L, 5L, 2L)).thenReturn("application/pdf");

        ResponseEntity<InputStreamResource> response = controller.downloadAttachment(10L, 5L, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("notes.pdf");
    }

    @Test
    void deleteAttachmentReturnsNoContent() {
        ResponseEntity<Void> response = controller.deleteAttachment(10L, 5L, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(appointmentDocumentService).deleteAttachment(10L, 5L, 2L);
    }
}
