/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.drivinglicense.service;

import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.ExtractionJson;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.workflow.domain.WorkflowType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Extraction schema for {@link WorkflowType#DRIVING_LICENSE_RECORD}: license number, authority,
 * dates and the classes (from the user's catalog).
 */
@Component
@RequiredArgsConstructor
public class DrivingLicenseRecordExtractionSchema implements DocumentExtractionSchema {

    private final DrivingLicenseCatalogService catalogService;

    @Override
    public String type() {
        return WorkflowType.DRIVING_LICENSE_RECORD.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        Map<String, DrivingLicenseDefinitionDto> byKey = new LinkedHashMap<>();
        for (DrivingLicenseDefinitionDto definition :
                catalogService.listAvailableDefinitionsForUser(userId)) {
            if (definition.isActive() && definition.getConfigKey() != null) {
                byKey.put(definition.getConfigKey(), definition);
            }
        }
        Map<String, String> keyToName = new LinkedHashMap<>();
        byKey.forEach((key, definition) -> keyToName.put(key, definition.getName()));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
                "issueDate",
                ExtractionJson.nullableDate(
                        "Date the license card was issued. EU card: field 4a (not the birth date in"
                                + " field 3)."));
        properties.put(
                "documentExpirationDate",
                ExtractionJson.nullableDate(
                        "Date until which the license card is valid. EU card: field 4b (later than"
                                + " 4a)."));
        properties.put("licenseNumber", ExtractionJson.nullableString("License number."));
        properties.put(
                "issuingAuthority",
                ExtractionJson.nullableString("Authority that issued the card."));
        if (!byKey.isEmpty()) {
            Map<String, Object> classItem = new LinkedHashMap<>();
            classItem.put("configKey", ExtractionJson.enumString(byKey.keySet()));
            classItem.put(
                    "grantDate",
                    ExtractionJson.nullableDate(
                            "Date this class was granted. EU card back: column 10 in the row of"
                                    + " the class (never the birth date)."));
            classItem.put(
                    "expirationDate",
                    ExtractionJson.nullableDate(
                            "Date this class expires, if stated. EU card back: column 11."));
            properties.put(
                    "classes",
                    ExtractionJson.arrayOf(
                            ExtractionJson.object(classItem),
                            "License classes printed on the document that exist in the catalog."));
        }

        LocalDate today = OrganizationWallClockTime.today();
        return new PreparedExtraction(
                "driving_license_record_extraction",
                "The document is a driving license, often an EU card: the front shows numbered"
                        + " fields (4a issue date, 4b expiry date, 5 license number, 4c authority), the"
                        + " back lists every class with its dates (column 9 class, 10 granted, 11"
                        + " expires). Take every class that has a date in column 10.\n"
                        + "Catalog of license classes:\n"
                        + ExtractionJson.catalogList(keyToName),
                ExtractionJson.object(properties),
                answer -> validate(answer, byKey, today));
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(
            JsonNode answer,
            Map<String, DrivingLicenseDefinitionDto> catalogByKey,
            LocalDate today) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<ExtractionHint> hints = new ArrayList<>();

        Optional<LocalDate> issue = ExtractionJson.date(answer.get("issueDate"));
        if (issue.isPresent() && issue.get().isAfter(today)) {
            hints.add(new ExtractionHint(ExtractionHint.FUTURE_DATE, "issueDate"));
            issue = Optional.empty();
        }
        issue.ifPresent(date -> fields.put("issueDate", date.toString()));

        Optional<LocalDate> expiration = ExtractionJson.date(answer.get("documentExpirationDate"));
        if (expiration.isPresent() && issue.isPresent() && !expiration.get().isAfter(issue.get())) {
            hints.add(new ExtractionHint(ExtractionHint.DATE_ORDER, "documentExpirationDate"));
            expiration = Optional.empty();
        }
        expiration.ifPresent(date -> fields.put("documentExpirationDate", date.toString()));

        ExtractionJson.text(answer.get("licenseNumber"), 128)
                .ifPresent(value -> fields.put("licenseNumber", value));
        ExtractionJson.text(answer.get("issuingAuthority"), 512)
                .ifPresent(value -> fields.put("issuingAuthority", value));

        JsonNode classes = answer.get("classes");
        if (classes != null && classes.isArray()) {
            List<Map<String, Object>> accepted = new ArrayList<>();
            Set<Long> seen = new LinkedHashSet<>();
            boolean rejected = false;
            for (JsonNode node : classes) {
                JsonNode keyNode = node.get("configKey");
                DrivingLicenseDefinitionDto definition =
                        keyNode != null && keyNode.isString()
                                ? catalogByKey.get(keyNode.asString())
                                : null;
                if (definition == null || !seen.add(definition.getId())) {
                    rejected = true;
                    continue;
                }
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("tenantDrivingLicenseDefinitionId", definition.getId());
                Optional<LocalDate> grant = ExtractionJson.date(node.get("grantDate"));
                if (grant.isPresent() && grant.get().isAfter(today)) {
                    hints.add(new ExtractionHint(ExtractionHint.FUTURE_DATE, "classes"));
                    grant = Optional.empty();
                }
                Optional<LocalDate> classExpiration =
                        ExtractionJson.date(node.get("expirationDate"));
                if (classExpiration.isPresent()
                        && grant.isPresent()
                        && !classExpiration.get().isAfter(grant.get())) {
                    hints.add(new ExtractionHint(ExtractionHint.DATE_ORDER, "classes"));
                    classExpiration = Optional.empty();
                }
                if (grant.isPresent() && issue.isPresent() && grant.get().isAfter(issue.get())) {
                    hints.add(new ExtractionHint(ExtractionHint.DATE_ORDER, "classes"));
                    grant = Optional.empty();
                }
                entry.put("grantDate", grant.map(LocalDate::toString).orElse(null));
                entry.put("expirationDate", classExpiration.map(LocalDate::toString).orElse(null));
                accepted.add(entry);
            }
            if (!accepted.isEmpty()) {
                fields.put("classes", accepted);
            }
            if (rejected) {
                hints.add(new ExtractionHint(ExtractionHint.VALUE_REJECTED, "classes"));
            }
        }

        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }
}
