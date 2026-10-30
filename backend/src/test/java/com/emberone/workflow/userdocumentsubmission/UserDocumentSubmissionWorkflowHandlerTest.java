/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.userdocumentsubmission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.UserDocumentSubmissionWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class UserDocumentSubmissionWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private UserDocumentSubmissionWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new UserDocumentSubmissionWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesStaticMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.USER_DOCUMENT_SUBMISSION);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.STATIC);
        assertThat(handler.payloadType()).isEqualTo(UserDocumentSubmissionWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
    }

    @Test
    void validateOpenRejectsEmptyTitle() {
        UserDocumentSubmissionWorkflowPayload payload =
                UserDocumentSubmissionWorkflowPayload.builder()
                        .title("   ")
                        .attachmentIds(List.of(100L))
                        .build();
        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void onOpenedAttachesDocuments() {
        Workflow workflow = Workflow.builder().id(5L).build();
        handler.onOpened(workflow);
        verify(workflowDocumentService).attachUserDocumentSubmissionAttachmentsToWorkflow(workflow);
    }
}
