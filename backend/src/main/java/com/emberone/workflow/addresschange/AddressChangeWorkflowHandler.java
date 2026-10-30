/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.addresschange;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.AddressChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class AddressChangeWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<AddressChangeWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.addressChange";

    private final WorkflowDocumentService workflowDocumentService;
    private final UserService userService;

    public AddressChangeWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            UserService userService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.userService = userService;
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.ADDRESS_CHANGE;
    }

    @Override
    public Class<AddressChangeWorkflowPayload> payloadType() {
        return AddressChangeWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - address change is not possible";
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        return resolveLocalizedDocumentPromotionMetadata(workflow, DOCUMENT_PROMOTION_PREFIX);
    }

    @Override
    public void validateOpen(WorkflowOpenContext<AddressChangeWorkflowPayload> ctx) {
        AddressChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        String street = trim(payload.getStreet());
        String city = trim(payload.getCity());
        String postalCode = trim(payload.getPostalCode());
        String country = trim(payload.getCountry());
        if (street.isEmpty() || city.isEmpty() || postalCode.isEmpty() || country.isEmpty()) {
            throw new WorkflowValidationException(
                    "Street, city, postal code and country must not be empty");
        }

        User user = requireOpeningUser(ctx);
        UserContactInformation contact = user.getContactInformation();
        String currentStreet = contact != null ? trim(contact.getStreet()) : "";
        String currentCity = contact != null ? trim(contact.getCity()) : "";
        String currentPostalCode = contact != null ? trim(contact.getPostalCode()) : "";
        String currentCountry = contact != null ? trim(contact.getCountry()) : "";

        if (street.equalsIgnoreCase(currentStreet)
                && city.equalsIgnoreCase(currentCity)
                && postalCode.equalsIgnoreCase(currentPostalCode)
                && country.equalsIgnoreCase(currentCountry)) {
            throw new WorkflowValidationException(
                    "The new address must differ from the current address");
        }

        payload.setStreet(street);
        payload.setCity(city);
        payload.setPostalCode(postalCode);
        payload.setCountry(country);
        payload.setPreviousStreet(currentStreet.isEmpty() ? null : currentStreet);
        payload.setPreviousCity(currentCity.isEmpty() ? null : currentCity);
        payload.setPreviousPostalCode(currentPostalCode.isEmpty() ? null : currentPostalCode);
        payload.setPreviousCountry(currentCountry.isEmpty() ? null : currentCountry);

        Long contextOrgId = resolveContextOrganization(ctx);
        validateWorkflowDraftAttachments(
                ctx.getOpenedByUserId(),
                contextOrgId,
                payload.getAttachmentIds(),
                AddressChangeWorkflowPayload.MAX_ATTACHMENTS,
                false);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<AddressChangeWorkflowPayload> ctx, Workflow existing) {
        AddressChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        String street = trim(payload.getStreet());
        String city = trim(payload.getCity());
        String postalCode = trim(payload.getPostalCode());
        String country = trim(payload.getCountry());
        if (street.isEmpty() || city.isEmpty() || postalCode.isEmpty() || country.isEmpty()) {
            throw new WorkflowValidationException(
                    "Street, city, postal code and country must not be empty");
        }

        User user = requireOpeningUser(ctx);
        UserContactInformation contact = user.getContactInformation();
        String currentStreet = contact != null ? trim(contact.getStreet()) : "";
        String currentCity = contact != null ? trim(contact.getCity()) : "";
        String currentPostalCode = contact != null ? trim(contact.getPostalCode()) : "";
        String currentCountry = contact != null ? trim(contact.getCountry()) : "";

        if (street.equalsIgnoreCase(currentStreet)
                && city.equalsIgnoreCase(currentCity)
                && postalCode.equalsIgnoreCase(currentPostalCode)
                && country.equalsIgnoreCase(currentCountry)) {
            throw new WorkflowValidationException(
                    "The new address must differ from the current address");
        }

        payload.setStreet(street);
        payload.setCity(city);
        payload.setPostalCode(postalCode);
        payload.setCountry(country);
        payload.setPreviousStreet(currentStreet.isEmpty() ? null : currentStreet);
        payload.setPreviousCity(currentCity.isEmpty() ? null : currentCity);
        payload.setPreviousPostalCode(currentPostalCode.isEmpty() ? null : currentPostalCode);
        payload.setPreviousCountry(currentCountry.isEmpty() ? null : currentCountry);

        Long contextOrgId = resolveContextOrganization(ctx);
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                contextOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                AddressChangeWorkflowPayload.MAX_ATTACHMENTS,
                false);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachAddressChangeAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachAddressChangeAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        AddressChangeWorkflowPayload payload =
                readPayload(workflow, AddressChangeWorkflowPayload.class);
        userService.applyApprovedAddressChange(
                workflow.getOpenedByUserId(),
                workflow.getTenantId(),
                payload.getStreet(),
                payload.getCity(),
                payload.getPostalCode(),
                payload.getCountry());
        log.info(
                "Applied approved address change for user {} via workflow {}",
                workflow.getOpenedByUserId(),
                workflow.getId());
    }
}
