/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.workflow.dto.CreateWorkflowCommentRequest;
import com.emberone.workflow.dto.WorkflowCommentDto;
import com.emberone.workflow.service.WorkflowCommentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class WorkflowCommentControllerTest {

    @Mock WorkflowCommentService commentService;
    @Mock MultipartFile file;

    @InjectMocks WorkflowCommentController controller;

    @Test
    void listCommentsDelegatesToService() {
        List<WorkflowCommentDto> comments = List.of(WorkflowCommentDto.builder().id(1L).build());
        when(commentService.listComments(9L)).thenReturn(comments);

        ResponseEntity<List<WorkflowCommentDto>> response = controller.listComments(9L);

        assertThat(response.getBody()).isEqualTo(comments);
    }

    @Test
    void createCommentDelegatesToService() {
        CreateWorkflowCommentRequest request = new CreateWorkflowCommentRequest();
        WorkflowCommentDto created = WorkflowCommentDto.builder().id(2L).build();
        when(commentService.createComment(9L, request)).thenReturn(created);

        ResponseEntity<WorkflowCommentDto> response = controller.createComment(9L, request);

        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void uploadCommentAttachmentDelegatesMultipartFile() {
        EntityAttachmentDto attachment = EntityAttachmentDto.builder().id(3L).build();
        when(commentService.uploadCommentAttachmentDraft(9L, file)).thenReturn(attachment);

        ResponseEntity<EntityAttachmentDto> response = controller.uploadCommentAttachment(9L, file);

        assertThat(response.getBody()).isEqualTo(attachment);
    }
}
