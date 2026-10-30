/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.security.TokenService;
import com.emberone.workflow.service.WorkflowDocumentService;
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
class WorkflowDocumentControllerTest {

    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock TokenService tokenService;
    @Mock MultipartFile file;

    WorkflowDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new WorkflowDocumentController(workflowDocumentService, tokenService);
    }

    @Test
    void uploadWorkflowDraftAttachmentRequiresUserId() {
        when(tokenService.requireUserId())
                .thenThrow(new IllegalStateException("No userId found in token"));

        assertThatThrownBy(() -> controller.uploadWorkflowDraftAttachment(2L, file))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uploadWorkflowDraftAttachmentDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(8L);
        EntityAttachmentDto created = EntityAttachmentDto.builder().id(1L).build();
        when(workflowDocumentService.uploadWorkflowAttachmentDraft(8L, 2L, file))
                .thenReturn(created);

        ResponseEntity<EntityAttachmentDto> response =
                controller.uploadWorkflowDraftAttachment(2L, file);

        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void listWorkflowAttachmentsDelegatesToService() {
        List<EntityAttachmentDto> attachments =
                List.of(EntityAttachmentDto.builder().id(3L).build());
        when(workflowDocumentService.listWorkflowAttachments(15L)).thenReturn(attachments);

        ResponseEntity<List<EntityAttachmentDto>> response =
                controller.listWorkflowAttachments(15L);

        assertThat(response.getBody()).isEqualTo(attachments);
    }

    @Test
    void downloadWorkflowAttachmentSetsHeaders() {
        InputStreamResource resource =
                new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        when(workflowDocumentService.downloadWorkflowAttachment(15L, 4L))
                .thenReturn(
                        new EntityAttachmentService.DownloadBundle(
                                resource, "proof.pdf", "application/pdf"));

        ResponseEntity<InputStreamResource> response =
                controller.downloadWorkflowAttachment(15L, 4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("proof.pdf");
    }
}
