/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentOutcome;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillment;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillmentEvent;
import com.emberone.platform.i18n.LocalizationService;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntryDocumentProofPdfRenderer {

    private static final SupportedLocale LOCALE = SupportedLocale.DE;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter INSTANT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(DISPLAY_ZONE)
                    .withLocale(Locale.GERMANY);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    private final LocalizationService localizationService;

    private String t(String key) {
        return localizationService.getMessage(key, LOCALE);
    }

    private String t(String key, Map<String, Object> params) {
        return localizationService.getInterpolatedMessage(key, params, LOCALE);
    }

    public byte[] render(
            List<UserEntryDocumentFulfillment> currentStates,
            List<UserEntryDocumentFulfillmentEvent> events,
            Map<Long, String> userDisplayNames,
            DocumentNameResolver documentNames,
            OrganizationNameResolver organizationNames) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(new Paragraph(t("entryDocumentProof.title"), TITLE_FONT));
            document.add(new Paragraph(" ", BODY_FONT));

            document.add(new Paragraph(t("entryDocumentProof.sectionCurrent"), SECTION_FONT));
            if (currentStates.isEmpty()) {
                document.add(new Paragraph(t("entryDocumentProof.noEntries"), BODY_FONT));
            } else {
                for (UserEntryDocumentFulfillment fulfillment : currentStates) {
                    appendCurrentState(document, fulfillment, documentNames, organizationNames);
                    document.add(new Paragraph(" ", BODY_FONT));
                }
            }

            document.add(new Paragraph(t("entryDocumentProof.sectionHistory"), SECTION_FONT));
            if (events.isEmpty()) {
                document.add(new Paragraph(t("entryDocumentProof.noEvents"), BODY_FONT));
            } else {
                int index = 1;
                for (UserEntryDocumentFulfillmentEvent event : events) {
                    appendEvent(
                            document,
                            index++,
                            event,
                            documentNames,
                            organizationNames,
                            userDisplayNames);
                    document.add(new Paragraph(" ", BODY_FONT));
                }
            }

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to render entry document proof PDF", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Failed to write entry document proof PDF", ex);
        }
    }

    private void appendCurrentState(
            Document document,
            UserEntryDocumentFulfillment fulfillment,
            DocumentNameResolver documentNames,
            OrganizationNameResolver organizationNames)
            throws DocumentException {
        document.add(
                labeledLine(
                        t("entryDocumentProof.document"),
                        documentNames.resolve(
                                fulfillment.getConfigKey(), fulfillment.getOrganizationId())));
        document.add(labeledLine(t("entryDocumentProof.configKey"), fulfillment.getConfigKey()));
        if (fulfillment.getOrganizationId() != null) {
            document.add(
                    labeledLine(
                            t("entryDocumentProof.organization"),
                            organizationNames.resolve(fulfillment.getOrganizationId())));
        }
        document.add(
                labeledLine(
                        t("entryDocumentProof.templateVersion"),
                        String.valueOf(fulfillment.getTemplateVersion())));
        document.add(labeledLine("SHA-256", nullToDash(fulfillment.getTemplateSha256Hex())));
        document.add(
                labeledLine(
                        t("entryDocumentProof.outcome"), formatOutcome(fulfillment.getOutcome())));
        document.add(labeledLine(t("entryDocumentProof.mode"), formatMode(fulfillment.getMode())));
        document.add(
                labeledLine(
                        t("entryDocumentProof.confirmedAt"),
                        formatInstant(fulfillment.getConfirmedAt())));
        if (fulfillment.getUserDocumentId() != null) {
            document.add(
                    labeledLine(
                            t("entryDocumentProof.userDocumentId"),
                            String.valueOf(fulfillment.getUserDocumentId())));
        }
    }

    private void appendEvent(
            Document document,
            int index,
            UserEntryDocumentFulfillmentEvent event,
            DocumentNameResolver documentNames,
            OrganizationNameResolver organizationNames,
            Map<Long, String> userDisplayNames)
            throws DocumentException {
        document.add(
                new Paragraph(t("entryDocumentProof.event", Map.of("index", index)), LABEL_FONT));
        document.add(
                labeledLine(
                        t("entryDocumentProof.document"),
                        documentNames.resolve(event.getConfigKey(), event.getOrganizationId())));
        document.add(labeledLine(t("entryDocumentProof.configKey"), event.getConfigKey()));
        if (event.getOrganizationId() != null) {
            document.add(
                    labeledLine(
                            t("entryDocumentProof.organization"),
                            organizationNames.resolve(event.getOrganizationId())));
        }
        document.add(
                labeledLine(
                        t("entryDocumentProof.templateVersion"),
                        String.valueOf(event.getTemplateVersion())));
        document.add(labeledLine("SHA-256", event.getTemplateSha256Hex()));
        document.add(
                labeledLine(t("entryDocumentProof.confirmationText"), event.getConfirmationText()));
        document.add(
                labeledLine(t("entryDocumentProof.outcome"), formatOutcome(event.getOutcome())));
        document.add(
                labeledLine(t("entryDocumentProof.channel"), formatChannel(event.getChannel())));
        document.add(
                labeledLine(
                        t("entryDocumentProof.confirmedAt"),
                        formatInstant(event.getConfirmedAt())));
        document.add(
                labeledLine(
                        t("entryDocumentProof.actor"),
                        formatUserReference(event.getActorUserId(), userDisplayNames)));
        if (event.getWorkflowReviewerUserId() != null) {
            document.add(
                    labeledLine(
                            t("entryDocumentProof.workflowReviewer"),
                            formatUserReference(
                                    event.getWorkflowReviewerUserId(), userDisplayNames)));
            document.add(
                    labeledLine(
                            t("entryDocumentProof.workflowReviewedAt"),
                            formatInstant(event.getWorkflowReviewedAt())));
        }
        if (event.getAdminRecordReason() != null && !event.getAdminRecordReason().isBlank()) {
            document.add(
                    labeledLine(t("entryDocumentProof.adminReason"), event.getAdminRecordReason()));
        }
    }

    private Paragraph labeledLine(String label, String value) {
        return new Paragraph(label + ": " + nullToDash(value), BODY_FONT);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "-";
        }
        return INSTANT_FORMAT.format(instant) + " (" + DISPLAY_ZONE.getId() + ")";
    }

    private String formatOutcome(EntryDocumentFulfillmentOutcome outcome) {
        return switch (outcome) {
            case ACCEPTED -> t("entryDocumentProof.outcome.ACCEPTED");
            case DECLINED -> t("entryDocumentProof.outcome.DECLINED");
            case WITHDRAWN -> t("entryDocumentProof.outcome.WITHDRAWN");
        };
    }

    private String formatChannel(EntryDocumentFulfillmentChannel channel) {
        return switch (channel) {
            case SELF -> t("entryDocumentProof.channel.SELF");
            case ADMIN_RECORDED -> t("entryDocumentProof.channel.ADMIN_RECORDED");
        };
    }

    private String formatMode(OnboardingDocumentFulfillmentMode mode) {
        return switch (mode) {
            case CONFIRMED -> t("entryDocumentProof.mode.CONFIRMED");
            case SIGNED_UPLOAD -> t("entryDocumentProof.mode.SIGNED_UPLOAD");
        };
    }

    private String formatUserReference(Long userId, Map<Long, String> userDisplayNames) {
        if (userId == null) {
            return "-";
        }
        String displayName = userDisplayNames.get(userId);
        if (displayName == null || displayName.isBlank()) {
            return t("entryDocumentProof.userFallback", Map.of("id", userId));
        }
        return displayName + " (#" + userId + ")";
    }

    @FunctionalInterface
    public interface DocumentNameResolver {
        String resolve(String configKey, Long organizationId);
    }

    @FunctionalInterface
    public interface OrganizationNameResolver {
        String resolve(long organizationId);
    }
}
