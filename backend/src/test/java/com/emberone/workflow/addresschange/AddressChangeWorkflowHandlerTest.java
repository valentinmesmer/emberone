/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.addresschange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.AddressChangeWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AddressChangeWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock UserService userService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private AddressChangeWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new AddressChangeWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        userService,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesStaticAddressChangeMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.ADDRESS_CHANGE);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.STATIC);
        assertThat(handler.payloadType()).isEqualTo(AddressChangeWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isFalse();
        assertThat(
                        handler.resolveDocumentPromotionDefaults(Workflow.builder().build())
                                .isIncludeWorkflowMedia())
                .isTrue();
    }

    @Test
    void validateOpenNormalizesAddressSnapshotsCurrentAddressAndChecksAttachmentOwnership() {
        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .contactInformation(
                                UserContactInformation.builder()
                                        .street("Old Street")
                                        .city("Old City")
                                        .postalCode("10000")
                                        .country("DE")
                                        .build())
                        .build();
        Organization organization = Organization.builder().id(10L).build();
        AddressChangeWorkflowPayload payload =
                AddressChangeWorkflowPayload.builder()
                        .street(" New Street ")
                        .city(" Berlin ")
                        .postalCode(" 10115 ")
                        .country(" DE ")
                        .attachmentIds(List.of(20L))
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));
        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        20L, EntityAttachmentType.WORKFLOW_DRAFT, 1L, 10L))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(1L).build()));

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getStreet()).isEqualTo("New Street");
        assertThat(payload.getPreviousStreet()).isEqualTo("Old Street");
        assertThat(payload.getCity()).isEqualTo("Berlin");
    }

    @Test
    void validateOpenRejectsMissingUserWrongTenantUnchangedAddressAndInvalidAttachments() {
        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, null)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Payload is required");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("A")
                                                        .city("B")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("User not found");

        User user =
                User.builder()
                        .id(1L)
                        .tenantId(7L)
                        .contactInformation(
                                UserContactInformation.builder()
                                        .street("Same")
                                        .city("City")
                                        .postalCode("1")
                                        .country("DE")
                                        .build())
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                8L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("New")
                                                        .city("City")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Tenant");
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street(" ")
                                                        .city("B")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("same")
                                                        .city("city")
                                                        .postalCode("1")
                                                        .country("de")
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("differ");

        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("New")
                                                        .city("City")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .attachmentIds(List.of(2L, 2L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Duplicate");

        List<Long> tooMany =
                IntStream.rangeClosed(1, AddressChangeWorkflowPayload.MAX_ATTACHMENTS + 1)
                        .mapToObj(Long::valueOf)
                        .toList();
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("New")
                                                        .city("City")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .attachmentIds(tooMany)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("At most");

        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        99L, EntityAttachmentType.WORKFLOW_DRAFT, 1L, 10L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("New")
                                                        .city("City")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .attachmentIds(List.of(99L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Attachment not found");

        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        20L, EntityAttachmentType.WORKFLOW_DRAFT, 1L, 10L))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(2L).build()));
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("New")
                                                        .city("City")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .attachmentIds(List.of(20L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not uploaded by you");
    }

    @Test
    void validateOpenTreatsMissingContactInformationAsEmptyPreviousAddress() {
        User user = User.builder().id(1L).tenantId(7L).build();
        AddressChangeWorkflowPayload payload =
                AddressChangeWorkflowPayload.builder()
                        .street("New Street")
                        .city("Berlin")
                        .postalCode("10115")
                        .country("DE")
                        .attachmentIds(null)
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getPreviousStreet()).isNull();
        assertThat(payload.getPreviousCity()).isNull();
        assertThat(payload.getPostalCode()).isEqualTo("10115");
    }

    @Test
    void resolveContextOrganizationRequiresMainMembership() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                handler.resolveContextOrganization(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                AddressChangeWorkflowPayload.builder()
                                                        .street("A")
                                                        .city("B")
                                                        .postalCode("1")
                                                        .country("DE")
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("No organization membership");
    }

    @Test
    void openedWorkflowAttachesDocumentsAndApprovedWorkflowAppliesParsedAddress() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload(
                                "{\"street\":\"Main 1\",\"city\":\"Berlin\",\"postalCode\":\"10115\",\"country\":\"DE\"}")
                        .build();

        handler.onOpened(workflow);
        handler.onApproved(workflow);

        verify(workflowDocumentService).attachAddressChangeAttachmentsToWorkflow(workflow);
        verify(userService).applyApprovedAddressChange(1L, 7L, "Main 1", "Berlin", "10115", "DE");
    }

    @Test
    void onApprovedRejectsMissingOrInvalidPayload() {
        Workflow missing =
                Workflow.builder().id(5L).tenantId(7L).openedByUserId(1L).payload(" ").build();
        Workflow invalid =
                Workflow.builder().id(6L).tenantId(7L).openedByUserId(1L).payload("{").build();

        assertThatThrownBy(() -> handler.onApproved(missing))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("payload missing");
        assertThatThrownBy(() -> handler.onApproved(invalid))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Invalid workflow payload");
    }
}
